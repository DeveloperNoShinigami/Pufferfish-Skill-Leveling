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

    @Unique
    private final Map<String, Long> addon$paidLevels = new HashMap<>();

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
    public long addon$getPaidLevels(String skillId) {
        return addon$paidLevels.getOrDefault(skillId, 0L);
    }

    @Override
    public void addon$setPaidLevels(String skillId, long bits) {
        if (bits == 0) {
            addon$paidLevels.remove(skillId);
        } else {
            addon$paidLevels.put(skillId, bits);
        }
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
            // Clear paid levels bitset
            addon$paidLevels.remove(skillId);
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

        if (addon$owner != null) {
            net.bluelotuscoding.skillleveling.manager.CategoryLockManager.updateLocks(addon$owner);
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

        // MANUALLY HANDLE ALL UNLOCKS (Level 0->1, 1->2, etc)
        // We do this to ensure consistent behavior and to fix the issue where
        // Pufferfish triggers rewards *before* we set our level to 1.

        addon$setSkillLevel(id, newLevel);

        // Mark this specific level as PAID since it came through unlockSkill (tree
        // purchase)
        long currentBits = addon$getPaidLevels(id);
        addon$setPaidLevels(id, currentBits | (1L << newLevel));

        if (addon$owner != null && addon$categoryId != null) {
            var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
            manager.getPerLevelRewardsReward(addon$categoryId, id).ifPresent(reward -> {
                reward.update(new net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl(addon$owner, newLevel,
                        true));
            });
        }

        // Cancel original Pufferfish unlock logic to prevent double-handling
        // (We already added it to the set in setSkillLevel)
        ci.cancel();
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
        addon$paidLevels.clear();
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
                    || leveledConfig.lootMode.equals("imbue_only") || leveledConfig.lootMode.equals("both"))) {

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
     * Intercept getSkillState with a clean priority system:
     * 1. Prerequisites First: If custom prerequisites fail → LOCKED (regardless of level/type).
     * 2. Mastery Second: If max level reached → UNLOCKED (Gold).
     * 3. Toggle Logic Third:
     *    - Loot/Imbue toggle at Level 0 → LOCKED.
     *    - Standard Toggle / Learned Hybrid → AVAILABLE/AFFORDABLE (bypasses parents).
     * 4. Default: Pufferfish logic for everything else.
     */
    @Inject(method = "getSkillState", at = @At("HEAD"), cancellable = true)
    private void onGetSkillState(CategoryConfig category, SkillConfig skill, SkillDefinitionConfig definition,
            CallbackInfoReturnable<Skill.State> cir) {
        int level = addon$getSkillLevel(skill.id());
        int maxLevel = addon$getMaxLevelFromDefinition(category, skill);
        var leveledConfig = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skill.id());

        int totalLevel;
        if (addon$owner != null) {
            int bonus = SkillLevelingMod.getInstance().getSkillLevelingManager()
                    .calculateEquipmentBonus(addon$owner, category.id(), skill.id());
            totalLevel = level + bonus;
        } else {
            totalLevel = level;
        }

        // ── Priority 1: Prerequisites ──
        // If custom prerequisites fail, the skill is LOCKED (regardless of level/type).
        if (leveledConfig != null && !leveledConfig.requiredSkills.isEmpty()) {
            boolean allMet = true;
            for (var req : leveledConfig.requiredSkills) {
                String reqCategoryId = req.categoryId != null ? req.categoryId : category.id().toString();
                int reqLevel = addon$getSkillLevelCrossCategory(reqCategoryId, req.skillId);
                if (reqLevel < req.minLevel) {
                    allMet = false;
                    break;
                }
            }
            if (!allMet) {
                cir.setReturnValue(Skill.State.LOCKED);
                return;
            }
        }

        // ── Priority 2: Mastery ──
        // If max level is reached and NOT a toggle, the skill is UNLOCKED (Gold).
        // Toggle skills skip mastery — they remain togglable (handled in Priority 3).
        if (maxLevel > 0 && totalLevel >= maxLevel && (leveledConfig == null || !leveledConfig.toggle)) {
            cir.setReturnValue(Skill.State.UNLOCKED);
            return;
        }

        // ── Priority 3: Toggle Logic ──
        if (leveledConfig != null && leveledConfig.toggle) {
            boolean isLootLearned = leveledConfig.lootMode != null && !leveledConfig.lootMode.isEmpty();

            if (isLootLearned && totalLevel == 0) {
                // Loot/Imbue toggle at Level 0 → LOCKED (not yet found/learned)
                cir.setReturnValue(Skill.State.LOCKED);
                return;
            }

            // Standard Toggle / Learned Hybrid → AVAILABLE/AFFORDABLE
            // Bypasses parent skill checks so toggles are always accessible
            if (addon$owner != null && level < maxLevel
                    && net.bluelotuscoding.skillleveling.points.SkillPointManager
                            .canAffordLevel(addon$owner, category.id(), skill.id(), level + 1)) {
                cir.setReturnValue(Skill.State.AFFORDABLE);
            } else {
                cir.setReturnValue(Skill.State.AVAILABLE);
            }
            return;
        }

        // ── Priority 4: Default ──
        // For skills with progression but not yet mastered:
        if (totalLevel > 0 || level > 0) {
            if (level < maxLevel) {
                if (addon$owner != null && net.bluelotuscoding.skillleveling.points.SkillPointManager
                        .canAffordLevel(addon$owner, category.id(), skill.id(), level + 1)) {
                    cir.setReturnValue(Skill.State.AFFORDABLE);
                } else {
                    cir.setReturnValue(Skill.State.AVAILABLE);
                }
            } else {
                // Base at max, but total isn't (e.g., equipment penalty) → still active
                cir.setReturnValue(Skill.State.UNLOCKED);
            }
            return;
        }
        // Level 0 non-toggle: falls through to Pufferfish default (parent checks, etc.)
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
            int currentLevel = entry.getValue();
            if (currentLevel > 0) {
                var skillOpt = category.skills().getById(skillId);
                if (skillOpt.isPresent()) {
                    var defOpt = category.definitions().getById(skillOpt.get().definitionId());
                    if (defOpt.isPresent()) {
                        long bits = addon$getPaidLevels(skillId);
                        int pointsPerLevel = defOpt.get().cost();

                        // Check for PerLevelRewardsReward to handle dynamic point costs per level
                        PerLevelRewardsReward foundPlr = null;
                        for (var reward : defOpt.get().rewards()) {
                            if (reward.instance() instanceof PerLevelRewardsReward plr) {
                                if (plr.getSkillId() == null || plr.getSkillId().equals(skillId)) {
                                    foundPlr = plr;
                                    break;
                                }
                            }
                        }

                        if (foundPlr != null) {
                            int skillCost = 0;
                            // Only sum costs for levels that are marked as PAID in our bitset
                            for (int i = 1; i <= currentLevel; i++) {
                                if ((bits & (1L << i)) != 0) {
                                    skillCost += foundPlr.getEffectivePointsPerLevel(i);
                                }
                            }
                            total += skillCost;
                        } else {
                            // Fallback for default cost (e.g. if PerkLevelRewards is missing but cost is
                            // set)
                            int paidCount = 0;
                            for (int i = 1; i <= currentLevel; i++) {
                                if ((bits & (1L << i)) != 0) {
                                    paidCount++;
                                }
                            }
                            total += pointsPerLevel * paidCount;
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

        NbtCompound paidNbt = new NbtCompound();
        for (Map.Entry<String, Long> entry : addon$paidLevels.entrySet()) {
            paidNbt.putLong(entry.getKey(), entry.getValue());
        }
        nbt.put("addon_paid_levels", paidNbt);
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

            if (nbt.contains("addon_paid_levels", NbtElement.COMPOUND_TYPE)) {
                var paidNbt = nbt.getCompound("addon_paid_levels");
                for (var key : paidNbt.getKeys()) {
                    ext.addon$setPaidLevels(key, paidNbt.getLong(key));
                }
            } else {
                // Migration: If no paid levels saved but we have skills, assume all current
                // levels were paid
                // (This matches old behavior where everything was assumed paid)
                for (String skillId : categoryData.getUnlockedSkillIds()) {
                    int level = ext.addon$getSkillLevel(skillId);
                    if (level > 0) {
                        long bits = 0;
                        for (int i = 1; i <= level; i++) {
                            bits |= (1L << i);
                        }
                        ext.addon$setPaidLevels(skillId, bits);
                    }
                }
            }

            // JOIN-TIME INITIALIZATION: If we have an owner and category ID, initialize
            // rewards immediately
            // to prevent join-time re-triggering.
            var owner = ext.addon$getOwner();
            var categoryId = ext.addon$getCategoryId();
            if (owner != null && categoryId != null) {
                SkillLevelingMod.getInstance().getSkillLevelingManager()
                        .initializeRewardsForCategory(owner, categoryId);
            }
        }
    }

    @Unique
    private int addon$getMaxLevelFromDefinition(CategoryConfig category, SkillConfig skill) {
        // Check LeveledConfigStorage first — it has the authoritative parsed value
        var leveledConfig = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skill.id());
        if (leveledConfig != null && leveledConfig.maxLevels > 0) {
            return leveledConfig.maxLevels;
        }

        var definitionOpt = category.definitions().getById(skill.definitionId());
        if (definitionOpt.isEmpty()) {
            // If config says 0 (pure toggle), respect that
            return leveledConfig != null ? leveledConfig.maxLevels : 1;
        }
        var definition = definitionOpt.get();

        // Check for PerLevelRewardsReward at top level AND nested inside toggle rewards
        int maxLevel = leveledConfig != null ? leveledConfig.maxLevels : 1;
        for (var reward : definition.rewards()) {
            var inst = reward.instance();
            if (inst instanceof PerLevelRewardsReward plr) {
                if (plr.getSkillId() == null || plr.getSkillId().equals(skill.id())) {
                    maxLevel = Math.max(maxLevel, plr.getMaxLevel());
                }
            } else if (inst instanceof net.bluelotuscoding.skillleveling.rewards.ToggleReward toggleReward) {
                // Search inside toggle's enable_rewards for nested PerLevelRewardsReward
                for (var enableReward : toggleReward.getEnableRewards()) {
                    if (enableReward.instance() instanceof PerLevelRewardsReward plr) {
                        if (plr.getSkillId() == null || plr.getSkillId().equals(skill.id())) {
                            maxLevel = Math.max(maxLevel, plr.getMaxLevel());
                        }
                    }
                }
            }
        }
        return maxLevel;
    }
}
