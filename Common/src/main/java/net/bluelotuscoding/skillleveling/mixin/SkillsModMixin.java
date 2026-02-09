package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.mixin_interface.CategoryDataExtension;
import net.bluelotuscoding.skillleveling.mixin_interface.PlayerDataExtension;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.server.data.CategoryData;
import net.puffish.skillsmod.server.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into ORIGINAL SkillsMod (0.17.1) to:
 * 1. Set player owner on PlayerData when retrieved
 * 2. Sync skill levels to client after skill unlock/lock operations
 * 
 * NOTE: Only target methods that exist in the ORIGINAL mod, not fork additions.
 */
@Mixin(value = SkillsMod.class)
public abstract class SkillsModMixin {

    /**
     * Hook into getPlayerData to set the player owner on the PlayerData.
     */
    @Inject(method = "getPlayerData", at = @At("RETURN"))
    private void onGetPlayerData(ServerPlayerEntity player, CallbackInfoReturnable<PlayerData> cir) {
        PlayerData data = cir.getReturnValue();
        if (data != null) {
            ((PlayerDataExtension) (Object) data).addon$setOwner(player);
        }
    }

    /**
     * Hook into tryUnlockSkill at HEAD to check if this is first unlock or
     * subsequent.
     * If skill already has level > 0, we cancel and handle it ourselves without
     * reward triggers.
     */
    @Inject(method = "tryUnlockSkill", at = @At("HEAD"), cancellable = true)
    private void onTryUnlockSkillHead(ServerPlayerEntity player, Identifier categoryId, String skillId, boolean force,
            CallbackInfo ci) {
        try {
            // Get category data to check current level
            var skillsMod = SkillsMod.getInstance();
            var getPlayerDataMethod = SkillsMod.class.getDeclaredMethod("getPlayerData", ServerPlayerEntity.class);
            getPlayerDataMethod.setAccessible(true);
            var playerData = (PlayerData) getPlayerDataMethod.invoke(skillsMod, player);

            if (playerData == null) {
                return; // Let original handle it
            }

            var getCategoryMethod = SkillsMod.class.getDeclaredMethod("getCategory", Identifier.class);
            getCategoryMethod.setAccessible(true);
            var categoryConfigOpt = (java.util.Optional<?>) getCategoryMethod.invoke(skillsMod, categoryId);

            if (categoryConfigOpt.isEmpty()) {
                return;
            }

            var categoryConfig = (net.puffish.skillsmod.config.CategoryConfig) categoryConfigOpt.get();
            var getOrCreateMethod = PlayerData.class.getDeclaredMethod("getOrCreateCategoryData",
                    net.puffish.skillsmod.config.CategoryConfig.class);
            getOrCreateMethod.setAccessible(true);
            var categoryData = (CategoryData) getOrCreateMethod.invoke(playerData, categoryConfig);

            if (categoryData instanceof CategoryDataExtension ext) {
                int currentLevel = ext.addon$getSkillLevel(skillId);

                // SUBSEQUENT LEVELING LOGIC (Level 1+)
                // We only need to intervene if the skill is already unlocked (currentLevel > 0)
                // OR if we need to block a specific loot_mode from being unlocked via the tree.

                // 1. LOOT MODE BLOCKING: Block tree unlock for tome_only or imbue_only skills
                var leveledConfig = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
                if (leveledConfig == null) {
                    var skillConfigOpt = categoryConfig.skills().getById(skillId);
                    if (skillConfigOpt.isPresent()) {
                        leveledConfig = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage
                                .get(skillConfigOpt.get().definitionId());
                    }
                }

                if (leveledConfig != null && !force && leveledConfig.lootMode != null) {
                    if (leveledConfig.lootMode.equals("tome_only") || leveledConfig.lootMode.equals("imbue_only")) {
                        ci.cancel();
                        return;
                    }
                }

                if (currentLevel > 0) {
                    // Already has levels - this is a subsequent unlock
                    // Check if we can add more levels
                    int maxLevel = ext.addon$getMaxLevelForSkill(skillId);
                    // ... (keep existing subsequent unlock logic)

                    // Get max from PerLevelRewardsReward
                    var skillConfigOpt = categoryConfig.skills().getById(skillId);
                    if (skillConfigOpt.isPresent()) {
                        var defOpt = categoryConfig.definitions().getById(skillConfigOpt.get().definitionId());
                        if (defOpt.isPresent()) {
                            for (var reward : defOpt.get().rewards()) {
                                if (reward
                                        .instance() instanceof net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward plr) {
                                    maxLevel = Math.max(maxLevel, plr.getMaxLevel());
                                }
                            }
                        }
                    }

                    if (currentLevel >= maxLevel) {
                        ci.cancel(); // At max, can't unlock more
                        return;
                    }

                    // Check level-specific prerequisites (required_skill_for_level)
                    int targetLevel = currentLevel + 1;
                    var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
                    if (!manager.checkLevelPrerequisites(player.getUuid(), categoryId, skillId, targetLevel, true)) {
                        ci.cancel(); // Level prerequisites not met (message sent by manager)
                        return;
                    }

                    // Get points_per_level for deduction
                    int pointsPerLevel = 0;
                    var skillConfigOpt2 = categoryConfig.skills().getById(skillId);
                    if (skillConfigOpt2.isPresent()) {
                        var defOpt2 = categoryConfig.definitions().getById(skillConfigOpt2.get().definitionId());
                        if (defOpt2.isPresent()) {
                            // First try to get from PerLevelRewardsReward
                            for (var reward : defOpt2.get().rewards()) {
                                if (reward
                                        .instance() instanceof net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward plr) {
                                    pointsPerLevel = plr.getPointsPerLevel();
                                    break;
                                }
                            }
                            // Fallback to definition cost if no per-level reward
                            if (pointsPerLevel <= 0) {
                                pointsPerLevel = defOpt2.get().cost();
                            }
                        }
                    }

                    // Get current points available
                    int pointsLeft = 0;
                    try {
                        var getPointsLeftMethod = CategoryData.class.getDeclaredMethod("getPointsLeft",
                                net.puffish.skillsmod.config.CategoryConfig.class);
                        getPointsLeftMethod.setAccessible(true);
                        pointsLeft = (int) getPointsLeftMethod.invoke(categoryData, categoryConfig);
                    } catch (Exception e) {
                        // Fallback or log error
                    }

                    if (pointsLeft < pointsPerLevel && !force) {
                        ci.cancel();
                        return;
                    }

                    // Increment level
                    ext.addon$incrementSkillLevel(skillId);

                    // Manually trigger ONLY the per-level rewards for the new level
                    int newLevel = ext.addon$getSkillLevel(skillId);
                    triggerPerLevelRewardsOnly(player, categoryConfig, skillId, newLevel);

                    // Deduct points (handled dynamically by CategoryDataMixin.getSpentPoints)
                    // We just need to sync the point change to the client
                    deductPoints(player, categoryConfig, categoryData, 0);

                    // Sync to client
                    var mod = SkillLevelingMod.getInstance();
                    if (mod != null) {
                        int totalLevel = mod.getSkillLevelingManager().getTotalSkillLevel(player, categoryId, skillId);
                        mod.getSkillLevelingManager().syncSkillLevelToClient(player, categoryId, skillId, newLevel,
                                totalLevel, maxLevel);
                    }

                    ci.cancel(); // Don't let original run - we handled it
                }
            }
        } catch (Exception e) {
            // On error, let original code handle it
        }
    }

    /**
     * Hook into tryUnlockSkill to sync level to client after unlock.
     * The original method sends SkillUpdateOutPacket with only boolean.
     * We send our own packet with the actual level.
     */
    @Inject(method = "tryUnlockSkill", at = @At("RETURN"))
    private void onTryUnlockSkill(ServerPlayerEntity player, Identifier categoryId, String skillId, boolean force,
            CallbackInfo ci) {
        syncSkillLevelAfterChange(player, categoryId, skillId);
    }

    /**
     * Hook into lockSkill to sync level 0 to client after lock.
     * This method EXISTS in the original mod.
     */
    @Inject(method = "lockSkill", at = @At("RETURN"))
    private void onLockSkill(ServerPlayerEntity player, Identifier categoryId, String skillId, CallbackInfo ci) {
        try {
            var mod = SkillLevelingMod.getInstance();
            if (mod != null) {
                // Send level 0 to indicate locked
                mod.getSkillLevelingManager().syncSkillLevelToClient(player, categoryId, skillId, 0, 0, 1, 0);

            }
        } catch (Exception e) {
            var logger = SkillLevelingMod.getInstance().getLogger();
            logger.error("Failed to sync skill lock: " + e.getMessage());
        }
    }

    /**
     * Helper method to sync skill level after any change operation.
     */
    private void syncSkillLevelAfterChange(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        try {
            var mod = SkillLevelingMod.getInstance();
            if (mod == null) {
                return;
            }

            // Use reflection to get the CategoryData
            var skillsMod = SkillsMod.getInstance();
            var getPlayerDataMethod = SkillsMod.class.getDeclaredMethod("getPlayerData", ServerPlayerEntity.class);
            getPlayerDataMethod.setAccessible(true);
            var playerData = (PlayerData) getPlayerDataMethod.invoke(skillsMod, player);

            if (playerData == null) {
                return;
            }

            var getCategoryMethod = SkillsMod.class.getDeclaredMethod("getCategory", Identifier.class);
            getCategoryMethod.setAccessible(true);
            var categoryConfigOpt = (java.util.Optional<?>) getCategoryMethod.invoke(skillsMod, categoryId);

            if (categoryConfigOpt.isEmpty()) {
                return;
            }

            var categoryConfig = (net.puffish.skillsmod.config.CategoryConfig) categoryConfigOpt.get();
            var getOrCreateMethod = PlayerData.class.getDeclaredMethod("getOrCreateCategoryData",
                    net.puffish.skillsmod.config.CategoryConfig.class);
            getOrCreateMethod.setAccessible(true);
            var categoryData = (CategoryData) getOrCreateMethod.invoke(playerData, categoryConfig);

            if (categoryData instanceof CategoryDataExtension ext) {
                int level = ext.addon$getSkillLevel(skillId);
                int maxLevel = 1;
                String definitionId = null;
                net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward perLevelReward = null;

                // Get max level and definition info from PerLevelRewardsReward
                var skillConfig = categoryConfig.skills().getById(skillId);
                if (skillConfig.isPresent()) {
                    definitionId = skillConfig.get().definitionId();
                    var defOpt = categoryConfig.definitions().getById(definitionId);
                    if (defOpt.isPresent()) {
                        for (var reward : defOpt.get().rewards()) {
                            if (reward
                                    .instance() instanceof net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward plr) {
                                maxLevel = Math.max(maxLevel, plr.getMaxLevel());
                                perLevelReward = plr;
                            }
                        }
                    }
                }

                int totalLevel = mod.getSkillLevelingManager().getTotalSkillLevel(player, categoryId, skillId);
                // Send our sync packet with the correct level, max level, and points per level
                int pointsPerLevelFinal = (perLevelReward != null) ? perLevelReward.getPointsPerLevel() : 1;
                mod.getSkillLevelingManager().syncSkillLevelToClient(player, categoryId, skillId, level, totalLevel,
                        maxLevel,
                        pointsPerLevelFinal, definitionId);

                // On first unlock (level 1), also sync descriptions to client
                if (level == 1 && perLevelReward != null) {
                    var levelDescs = perLevelReward.getLevelDescriptions();
                    var extraDescs = perLevelReward.getLevelExtraDescriptions();
                    boolean merge = perLevelReward.isMergeDescription();

                    if ((levelDescs != null && !levelDescs.isEmpty())
                            || (extraDescs != null && !extraDescs.isEmpty())) {
                        mod.getSkillLevelingManager().syncDescriptionsToClient(
                                player, definitionId, levelDescs, extraDescs, merge, maxLevel);
                    }
                }
            }
        } catch (Exception e) {
            var logger = SkillLevelingMod.getInstance().getLogger();
            logger.error("Failed to sync skill level: " + e.getMessage());
        }
    }

    /**
     * Hook into resetSkills to sync level=0 for all skills in the category.
     * This ensures the client knows all skills are now locked.
     */
    @Inject(method = "resetSkills", at = @At("RETURN"))
    private void onResetSkills(ServerPlayerEntity player, Identifier categoryId, CallbackInfo ci) {
        try {
            var mod = SkillLevelingMod.getInstance();
            if (mod == null) {
                return;
            }

            // Get category config to iterate all skills
            var skillsMod = SkillsMod.getInstance();
            var getCategoryMethod = SkillsMod.class.getDeclaredMethod("getCategory", Identifier.class);
            getCategoryMethod.setAccessible(true);
            var categoryConfigOpt = (java.util.Optional<?>) getCategoryMethod.invoke(skillsMod, categoryId);

            if (categoryConfigOpt.isEmpty()) {
                return;
            }

            var categoryConfig = (net.puffish.skillsmod.config.CategoryConfig) categoryConfigOpt.get();

            // Sync level=0 for all skills in the category
            for (var skill : categoryConfig.skills().getAll()) {
                mod.getSkillLevelingManager().syncSkillLevelToClient(player, categoryId, skill.id(), 0, 0, 1);
            }
        } catch (Exception e) {
            var logger = SkillLevelingMod.getInstance().getLogger();
            logger.error("Failed to sync skill reset: " + e.getMessage());
        }
    }

    /**
     * Hook into showCategory to sync skill levels to client.
     * This is called on player join for each unlocked category.
     */
    @Inject(method = "showCategory", at = @At("RETURN"))
    private void onShowCategory(ServerPlayerEntity player, net.puffish.skillsmod.config.CategoryConfig category,
            CategoryData categoryData, CallbackInfo ci) {
        try {
            var mod = SkillLevelingMod.getInstance();
            if (mod == null) {
                return;
            }

            for (var skill : category.skills().getAll()) {
                if (categoryData instanceof CategoryDataExtension ext) {
                    int level = ext.addon$getSkillLevel(skill.id());
                    int maxLevel = 1;
                    var defOpt = category.definitions().getById(skill.definitionId());
                    if (defOpt.isPresent()) {
                        for (var reward : defOpt.get().rewards()) {
                            if (reward
                                    .instance() instanceof net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward plr) {
                                maxLevel = Math.max(maxLevel, plr.getMaxLevel());
                            }
                        }
                    }
                    int totalLevel = mod.getSkillLevelingManager().getTotalSkillLevel(player, category.id(),
                            skill.id());

                    // SYNC FIX: Sync all skills, even at level 0, to ensure gear bonuses show up
                    mod.getSkillLevelingManager().syncSkillLevelToClient(player, category.id(), skill.id(), level,
                            totalLevel, maxLevel, skill.definitionId());
                }
            }

        } catch (Exception e) {
            SkillLevelingMod.getInstance().getLogger()
                    .error("Failed to sync skill levels on showCategory: " + e.getMessage());
        }
    }

    /**
     * Trigger ONLY the PerLevelRewardsReward for a specific level.
     * This is used for subsequent level-ups to avoid triggering base rewards.
     */
    private void triggerPerLevelRewardsOnly(ServerPlayerEntity player,
            net.puffish.skillsmod.config.CategoryConfig categoryConfig, String skillId, int level) {
        try {
            var skillConfigOpt = categoryConfig.skills().getById(skillId);
            if (skillConfigOpt.isEmpty()) {
                return;
            }

            var defOpt = categoryConfig.definitions().getById(skillConfigOpt.get().definitionId());
            if (defOpt.isEmpty()) {
                return;
            }

            for (var reward : defOpt.get().rewards()) {
                if (reward.instance() instanceof net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward plr) {
                    // Trigger the per-level reward for the new level
                    plr.update(new net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl(player, level, true));
                }
            }
        } catch (Exception e) {
            // Silently fail - rewards are not critical
        }
    }

    /**
     * Deduct points from the player for the level-up cost.
     * We don't directly modify points - instead, the getSpentPoints interceptor in
     * CategoryDataMixin handles this by multiplying cost * level.
     * 
     * However, we need to sync the point change to the client.
     * We do this by calling syncPoints which triggers the original sync mechanism.
     */
    private void deductPoints(ServerPlayerEntity player,
            net.puffish.skillsmod.config.CategoryConfig categoryConfig,
            CategoryData categoryData, int pointsToDeduct) {
        try {
            var skillsMod = SkillsMod.getInstance();

            // Call syncPoints to update the client with new point totals
            // This uses the original mod's sync mechanism
            var syncPointsMethod = SkillsMod.class.getDeclaredMethod("syncPoints",
                    ServerPlayerEntity.class,
                    net.puffish.skillsmod.config.CategoryConfig.class,
                    CategoryData.class);
            syncPointsMethod.setAccessible(true);
            syncPointsMethod.invoke(skillsMod, player, categoryConfig, categoryData);
        } catch (Exception e) {
            // If sync fails, the client may be out of sync but will correct on next action
            var logger = SkillLevelingMod.getInstance().getLogger();
            logger.error("Failed to sync points: " + e.getMessage());
        }
    }
}
