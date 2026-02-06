package net.bluelotuscoding.skillleveling.mixin;

import net.puffish.skillsmod.server.data.CategoryData;
import net.puffish.skillsmod.config.CategoryConfig;
import net.puffish.skillsmod.config.skill.SkillConfig;
import net.puffish.skillsmod.config.skill.SkillDefinitionConfig;
import net.puffish.skillsmod.api.Skill;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.bluelotuscoding.skillleveling.mixin_interface.CategoryDataExtension;

/**
 * Complete CategoryData transformation to support multi-level skills.
 * Uses a shadow Map to track actual levels while maintaining compatibility
 * with the original Set-based structure.
 */
@Mixin(value = CategoryData.class, remap = false)
public abstract class CategoryDataMixin implements CategoryDataExtension {

    @Shadow
    private Set<String> unlockedSkills;

    @Unique
    private final Map<String, Integer> addon$skillLevels = new HashMap<>();

    @Unique
    private ServerPlayerEntity addon$owner;

    @Unique
    private net.minecraft.util.Identifier addon$categoryId;

    // ===== CategoryDataExtension Implementation =====

    @Override
    public void addon$setOwner(ServerPlayerEntity player) {
        this.addon$owner = player;
    }

    @Override
    public ServerPlayerEntity addon$getOwner() {
        return this.addon$owner;
    }

    @Override
    public void addon$setCategoryId(net.minecraft.util.Identifier categoryId) {
        this.addon$categoryId = categoryId;
    }

    @Override
    public net.minecraft.util.Identifier addon$getCategoryId() {
        return this.addon$categoryId;
    }

    @Override
    public int addon$getSkillLevel(String skillId) {
        return addon$skillLevels.getOrDefault(skillId, 0);
    }

    /**
     * Get skill level from any category (supports cross-category prerequisites)
     */
    @Unique
    private int addon$getSkillLevelCrossCategory(String categoryIdStr, String skillId) {
        if (categoryIdStr == null || categoryIdStr.isEmpty()) {
            return addon$getSkillLevel(skillId);
        }

        if (addon$owner == null) {
            return 0;
        }

        try {
            net.minecraft.util.Identifier targetCategoryId = new net.minecraft.util.Identifier(categoryIdStr);
            return net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance()
                    .getSkillLevelingManager()
                    .getTotalSkillLevel(addon$owner, targetCategoryId, skillId);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void addon$setSkillLevel(String skillId, int level) {
        if (level <= 0) {
            addon$skillLevels.remove(skillId);
            unlockedSkills.remove(skillId);
            if (addon$owner != null) {
                // Also update persistent data manager
                SkillLevelingMod.getInstance().getSkillLevelingManager().getDataManager()
                        .clearSkillLevel(addon$owner, addon$categoryId, skillId);
            }
        } else {
            addon$skillLevels.put(skillId, level);
            unlockedSkills.add(skillId);
            if (addon$owner != null) {
                // Also update persistent data manager
                var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
                manager.getDataManager().setSkillLevel(addon$owner, addon$categoryId, skillId, level);

                // IMMEDIATE CLIENT SYNC: Notify client of level change so UI updates (mastery,
                // reveal, etc)
                int totalLevel = manager.getTotalSkillLevel(addon$owner, addon$categoryId, skillId);
                int maxLevel = manager.getMaxLevel(addon$categoryId, skillId);
                manager.syncSkillLevelToClient(addon$owner, addon$categoryId, skillId, level, totalLevel, maxLevel);
            }
        }
    }

    @Override
    public void addon$incrementSkillLevel(String skillId) {
        int currentLevel = addon$getSkillLevel(skillId);
        addon$setSkillLevel(skillId, currentLevel + 1);
    }

    @Override
    public boolean addon$decrementSkillLevel(String skillId) {
        int currentLevel = addon$getSkillLevel(skillId);
        if (currentLevel <= 1) {
            addon$setSkillLevel(skillId, 0);
            return false;
        }
        addon$setSkillLevel(skillId, currentLevel - 1);
        return true;
    }

    @Override
    public boolean addon$isSkillUnlocked(String skillId) {
        return addon$getSkillLevel(skillId) > 0;
    }

    @Override
    public int addon$getMaxLevelForSkill(String skillId) {
        // Default max level - will be overridden by checking PerLevelRewardsReward
        return 1;
    }

    // ===== Method Intercepts =====

    /**
     * Intercept unlockSkill to INCREMENT level instead of just adding to Set.
     * CRITICAL: Only let FIRST unlock (0→1) proceed through original code so base
     * rewards fire once.
     * Subsequent level-ups (1→2, 2→3, etc) should just increment our map and
     * cancel.
     */
    @Inject(method = "unlockSkill", at = @At("HEAD"), cancellable = true)
    private void onUnlockSkill(String id, CallbackInfo ci) {
        int currentLevel = addon$getSkillLevel(id);
        int newLevel = currentLevel + 1;

        if (currentLevel > 0) {
            // This is a subsequent level-up (1→2, 2→3, etc)
            addon$setSkillLevel(id, newLevel);

            // We MUST manually trigger rewards for Level 2+ since Pufferfish only triggers
            // awards when a skill is first added to the unlockedSet.
            if (addon$owner != null && addon$categoryId != null) {
                var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
                manager.getPerLevelRewardsReward(addon$categoryId, id).ifPresent(reward -> {
                    reward.update(new net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl(addon$owner, newLevel,
                            true));
                });
            }

            // Cancel to prevent Pufferfish from seeing this as a new unlock (it isn't)
            ci.cancel();
        }
        // If currentLevel == 0, this is first unlock - let original code run first
        // so Pufferfish's own reward triggers and set-addition work correctly.
    }

    /**
     * Set level to 1 after successful first unlock.
     */
    @Inject(method = "unlockSkill", at = @At("RETURN"))
    private void onUnlockSkillReturn(String id, CallbackInfo ci) {
        if (addon$getSkillLevel(id) == 0 && unlockedSkills.contains(id)) {
            addon$setSkillLevel(id, 1);
        }
    }

    /**
     * Intercept lockSkill to set level to 0.
     */
    @Inject(method = "lockSkill", at = @At("HEAD"), cancellable = true)
    private void onLockSkill(String id, CallbackInfo ci) {
        addon$setSkillLevel(id, 0);
        ci.cancel();
    }

    /**
     * Intercept resetSkills to clear our level map.
     */
    @Inject(method = "resetSkills", at = @At("HEAD"))
    private void onResetSkills(CallbackInfo ci) {
        addon$skillLevels.clear();
        if (addon$owner != null && addon$categoryId != null) {
            SkillLevelingMod.getInstance().getSkillLevelingManager().getDataManager()
                    .resetCategorySkillLevels(addon$owner, addon$categoryId);
        }
    }

    /**
     * Intercept canUnlockSkill to allow re-purchase of multi-level skills
     * and enforce prerequisites and loot_mode blocks.
     */
    @Inject(method = "canUnlockSkill", at = @At("HEAD"), cancellable = true)
    private void onCanUnlockSkill(CategoryConfig category, SkillConfig skill, boolean force,
            CallbackInfoReturnable<Boolean> cir) {
        int level = addon$getSkillLevel(skill.id());
        int maxLevel = addon$getMaxLevelFromDefinition(category, skill);

        // Ensure owner is resolved if possible
        if (addon$owner == null) {
            SkillLevelingMod.getInstance().getSkillLevelingManager().getServer().ifPresent(server -> {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    try {
                        var skillsMod = net.puffish.skillsmod.SkillsMod.getInstance();
                        var getPlayerDataMethod = net.puffish.skillsmod.SkillsMod.class
                                .getDeclaredMethod("getPlayerData", ServerPlayerEntity.class);
                        getPlayerDataMethod.setAccessible(true);
                        var data = getPlayerDataMethod.invoke(skillsMod, player);

                        if (data != null) {
                            var getOrCreateMethod = net.puffish.skillsmod.server.data.PlayerData.class
                                    .getDeclaredMethod("getOrCreateCategoryData",
                                            net.puffish.skillsmod.config.CategoryConfig.class);
                            getOrCreateMethod.setAccessible(true);
                            var catData = getOrCreateMethod.invoke(data, category);
                            if (catData == (Object) this) {
                                addon$setOwner(player);
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            });
        }
        var owner = addon$owner;

        var leveledConfig = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skill.id());
        if (leveledConfig != null && !force) {
            // 1. Check loot_mode
            if (leveledConfig.lootMode != null && (leveledConfig.lootMode.equals("tome_only")
                    || leveledConfig.lootMode.equals("imbue_only"))) {

                if (owner != null) {
                    owner.sendMessage(
                            net.minecraft.text.Text.literal(
                                    "§8[§6Skill Leveling§8] §cThis skill cannot be learned via the skill tree."),
                            false);
                    SkillLevelingMod.getInstance().getSkillLevelingManager().sendCloseScreenPacket(owner);
                }
                cir.setReturnValue(false);
                return;
            }

            // 2. Check BASE prerequisites (for level 0 -> 1)
            if (level == 0 && !leveledConfig.requiredSkills.isEmpty()) {
                List<String> missingPrereqs = new java.util.ArrayList<>();
                List<String> metPrereqs = new java.util.ArrayList<>();

                for (var reqSkill : leveledConfig.requiredSkills) {
                    String reqCategoryId = reqSkill.categoryId != null ? reqSkill.categoryId
                            : addon$categoryId.toString();
                    int reqLevel = addon$getSkillLevelCrossCategory(reqCategoryId, reqSkill.skillId);

                    String categoryDisplay = reqSkill.categoryId != null ? " (" + reqCategoryId + ")" : "";
                    String prereqDesc = reqSkill.skillId + categoryDisplay + " Lv" + reqSkill.minLevel;

                    if (reqLevel < reqSkill.minLevel) {
                        missingPrereqs.add("§c✗ " + prereqDesc + " §7[Current: " + reqLevel + "]");
                    } else {
                        metPrereqs.add("§a✓ " + prereqDesc);
                    }
                }

                if (!missingPrereqs.isEmpty()) {
                    if (owner != null && !force) {
                        StringBuilder msg = new StringBuilder("§8[§6Skill Leveling§8] §cPrerequisites not met:\n");
                        for (String missing : missingPrereqs) {
                            msg.append("§7 - ").append(missing).append("\n");
                        }
                        if (!metPrereqs.isEmpty()) {
                            msg.append("§7Met:\n");
                            for (String met : metPrereqs) {
                                msg.append("§8 - ").append(met).append("\n");
                            }
                        }
                        owner.sendMessage(net.minecraft.text.Text.literal(msg.toString().trim()), false);
                        SkillLevelingMod.getInstance().getSkillLevelingManager().sendCloseScreenPacket(owner);
                    }
                    cir.setReturnValue(false);
                    return;
                }
            }

            // 3. Check PER-LEVEL prerequisites (for progression)
            if (level > 0 && leveledConfig.requiredSkillsForLevel != null && !force) {
                int targetLevel = level + 1;
                var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
                if (owner != null) {
                    // Purchase attempt check: use notify=true
                    if (!manager.checkLevelPrerequisites(owner.getUuid(), category.id(), skill.id(), targetLevel,
                            true)) {
                        cir.setReturnValue(false);
                        return;
                    }
                } else {
                    // State check or owner unknown: use notify=false to avoid spam
                    if (!manager.checkLevelPrerequisites(null, category.id(), skill.id(), targetLevel, false)) {
                        // Note: we can't really check by UUID if UUID is null, but manager handles it
                    }
                }
            }
        }

        // 4. Check Affordability (Unified for all levels)
        if (!force) {
            if (owner != null && !net.bluelotuscoding.skillleveling.points.SkillPointManager
                    .canAffordLevel(owner, category.id(), skill.id(), level + 1)) {
                // Optional: send message if not affordable? (Original code just returns false)
                cir.setReturnValue(false);
                return;
            }
        }

        if (level >= maxLevel) {
            cir.setReturnValue(false);
            return;
        }

        cir.setReturnValue(true);
    }

    /**
     * Intercept getSkillState to properly check level vs maxLevel.
     */
    @Inject(method = "getSkillState", at = @At("HEAD"), cancellable = true)
    private void onGetSkillState(CategoryConfig category, SkillConfig skill, SkillDefinitionConfig definition,
            CallbackInfoReturnable<Skill.State> cir) {
        int level = addon$getSkillLevel(skill.id());
        int maxLevel = addon$getMaxLevelFromDefinition(category, skill);

        int totalLevel;
        if (addon$owner != null) {
            int bonus = SkillLevelingMod.getInstance().getSkillLevelingManager()
                    .calculateEquipmentBonus(addon$owner, category.id(), skill.id());
            totalLevel = level + bonus;
        } else {
            // Fallback for client if needed (though ClientCategoryDataMixin usually handles
            // it)
            totalLevel = level;
        }

        // Mastery Check: ONLY show as UNLOCKED (Golden highlight) if at max level
        if (maxLevel > 0 && totalLevel >= maxLevel) {
            cir.setReturnValue(Skill.State.UNLOCKED);
            return;
        }

        // Progression Check: If we have progress but not at max level
        if (totalLevel > 0 || level > 0) {
            var leveledConfig = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skill.id());

            // For loot-only skills (imbue_only, tome_only):
            // Show as UNLOCKED if they are Level 1+ but not maxed (no border, active icon)
            if (leveledConfig != null && leveledConfig.lootMode != null
                    && (leveledConfig.lootMode.equals("tome_only") || leveledConfig.lootMode.equals("imbue_only"))) {
                cir.setReturnValue(Skill.State.UNLOCKED);
                return;
            }

            // For normal skills:
            if (level < maxLevel) {
                if (addon$owner != null && net.bluelotuscoding.skillleveling.points.SkillPointManager
                        .canAffordLevel(addon$owner, category.id(), skill.id(), level + 1)) {
                    cir.setReturnValue(Skill.State.AFFORDABLE);
                } else {
                    cir.setReturnValue(Skill.State.AVAILABLE);
                }
            } else {
                // Base at max, but total isn't (penalty) -> show as active
                cir.setReturnValue(Skill.State.UNLOCKED);
            }
            return;
        }
    }

    /**
     * Intercept countUnlocked to sum LEVELS, not just count entries.
     */
    @Inject(method = "countUnlocked", at = @At("HEAD"), cancellable = true)
    private void onCountUnlocked(CategoryConfig category, String definitionId,
            CallbackInfoReturnable<Integer> cir) {
        int sum = 0;
        for (var skillConfig : category.skills().getAll()) {
            if (skillConfig.definitionId().equals(definitionId)) {
                sum += addon$getSkillLevel(skillConfig.id());
            }
        }
        cir.setReturnValue(sum);
    }

    /**
     * Intercept getSpentPoints to properly calculate points spent including
     * multi-level skills.
     */
    @Inject(method = "getSpentPoints", at = @At("HEAD"), cancellable = true)
    private void onGetSpentPoints(CategoryConfig category,
            CallbackInfoReturnable<Integer> cir) {
        int total = 0;
        for (var entry : addon$skillLevels.entrySet()) {
            String skillId = entry.getKey();
            int level = entry.getValue();
            if (level > 0) {
                var skillOpt = category.skills().getById(skillId);
                if (skillOpt.isPresent()) {
                    var defOpt = category.definitions().getById(skillOpt.get().definitionId());
                    if (defOpt.isPresent()) {
                        int pointsPerLevel = defOpt.get().cost();

                        for (var reward : defOpt.get().rewards()) {
                            if (reward.instance() instanceof PerLevelRewardsReward plr) {
                                if (plr.getSkillId() == null || plr.getSkillId().equals(skillId)) {
                                    int skillCost = 0;
                                    for (int i = 1; i <= level; i++) {
                                        skillCost += plr.getEffectivePointsPerLevel(i);
                                    }
                                    total += skillCost;
                                    pointsPerLevel = -1; // Flag found
                                    break;
                                }
                            }
                        }

                        if (pointsPerLevel != -1) {
                            total += pointsPerLevel * level;
                        }
                    }
                }
            }
        }
        cir.setReturnValue(total);
    }

    /**
     * Intercept writeNbt to ensure our custom levels are saved.
     */
    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void onWriteNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        NbtCompound levelsNbt = new NbtCompound();
        for (Map.Entry<String, Integer> entry : addon$skillLevels.entrySet()) {
            levelsNbt.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("addon_skill_levels", levelsNbt);
    }

    /**
     * Intercept read (static factory) to load skill levels.
     */
    @Inject(method = "read", at = @At("RETURN"))
    private static void onRead(NbtCompound nbt, CallbackInfoReturnable<CategoryData> cir) {
        var categoryData = cir.getReturnValue();
        if (categoryData instanceof CategoryDataExtension ext) {
            if (nbt.contains("addon_skill_levels", NbtElement.COMPOUND_TYPE)) {
                var levelsNbt = nbt.getCompound("addon_skill_levels");
                for (var key : levelsNbt.getKeys()) {
                    ext.addon$setSkillLevel(key, levelsNbt.getInt(key));
                }
            } else {
                for (String skillId : categoryData.getUnlockedSkillIds()) {
                    if (ext.addon$getSkillLevel(skillId) == 0) {
                        ext.addon$setSkillLevel(skillId, 1);
                    }
                }
            }
        }
    }

    @Unique
    private int addon$getMaxLevelFromDefinition(CategoryConfig category, SkillConfig skill) {
        var definitionOpt = category.definitions().getById(skill.definitionId());
        if (definitionOpt.isEmpty()) {
            return 1;
        }
        var definition = definitionOpt.get();

        int maxLevel = 1;
        for (var reward : definition.rewards()) {
            var inst = reward.instance();
            if (inst instanceof PerLevelRewardsReward plr) {
                if (plr.getSkillId() == null || plr.getSkillId().equals(skill.id())) {
                    maxLevel = Math.max(maxLevel, plr.getMaxLevel());
                }
            }
        }
        return maxLevel;
    }
}
