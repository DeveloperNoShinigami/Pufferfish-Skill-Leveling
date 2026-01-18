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
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
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

    /**
     * Shadow map to track actual skill levels.
     * The original unlockedSkills Set only knows "unlocked or not".
     * This map tracks the actual level (1, 2, 3, etc.) per skill.
     */
    @Unique
    private Map<String, Integer> addon$skillLevels = new HashMap<>();

    @Unique
    private ServerPlayerEntity addon$owner;

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
    public int addon$getSkillLevel(String skillId) {
        return addon$skillLevels.getOrDefault(skillId, 0);
    }

    @Override
    public void addon$setSkillLevel(String skillId, int level) {
        if (level <= 0) {
            addon$skillLevels.remove(skillId);
            unlockedSkills.remove(skillId);
        } else {
            addon$skillLevels.put(skillId, level);
            unlockedSkills.add(skillId); // Keep original Set in sync
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
        addon$incrementSkillLevel(id);

        if (currentLevel > 0) {
            // This is a subsequent level-up (1→2, 2→3, etc)
            // Cancel to prevent base rewards from firing again
            // The skill is already in unlockedSkills Set, so original code would do nothing
            // anyway
            ci.cancel();
        }
        // If currentLevel == 0, this is first unlock - let original code run to:
        // 1. Add to unlockedSkills Set
        // 2. (The reward triggering happens in SkillsMod.tryUnlockSkill, not here)
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
    }

    /**
     * Intercept canUnlockSkill to allow re-purchase of multi-level skills.
     * CRITICAL: The original method returns false if skill is already unlocked.
     * We must override this to allow purchasing additional levels.
     */
    @Inject(method = "canUnlockSkill", at = @At("HEAD"), cancellable = true)
    private void onCanUnlockSkill(CategoryConfig category, SkillConfig skill, boolean force,
            CallbackInfoReturnable<Boolean> cir) {
        int currentLevel = addon$getSkillLevel(skill.id());
        int maxLevel = addon$getMaxLevelFromDefinition(category, skill);

        if (currentLevel > 0) {
            // Skill already has levels
            if (currentLevel >= maxLevel) {
                // At max level - can't unlock more
                cir.setReturnValue(false);
                return;
            }

            // Not at max level - allow re-purchase
            // Force means admin/command usage - always allow
            // Otherwise, the original affordability check would work,
            // BUT the original method will return false because skill is "already unlocked"
            // So we MUST return true here to allow it
            cir.setReturnValue(true);
        }
        // If level is 0, let original method handle first unlock
    }

    /**
     * Intercept getSkillState to properly check level vs maxLevel.
     */
    @Inject(method = "getSkillState", at = @At("HEAD"), cancellable = true)
    private void onGetSkillState(CategoryConfig category, SkillConfig skill, SkillDefinitionConfig definition,
            CallbackInfoReturnable<Skill.State> cir) {
        int level = addon$getSkillLevel(skill.id());
        int maxLevel = addon$getMaxLevelFromDefinition(category, skill);

        if (level >= maxLevel && level > 0) {
            // Fully maxed out - show as UNLOCKED (highlighted)
            cir.setReturnValue(Skill.State.UNLOCKED);
        }
        // Otherwise let original method determine state (AFFORDABLE, AVAILABLE, LOCKED,
        // etc.)
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
     * Uses points_per_level from PerLevelRewardsReward config, not base cost.
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
                        // Get points_per_level from PerLevelRewardsReward, fallback to base cost
                        int pointsPerLevel = defOpt.get().cost(); // Default fallback

                        // Look for PerLevelRewardsReward to get actual points_per_level
                        for (var reward : defOpt.get().rewards()) {
                            if (reward.instance() instanceof PerLevelRewardsReward plr) {
                                if (plr.getSkillId() == null || plr.getSkillId().equals(skillId)) {
                                    pointsPerLevel = plr.getPointsPerLevel();
                                    break;
                                }
                            }
                        }

                        total += pointsPerLevel * level;
                    }
                }
            }
        }
        cir.setReturnValue(total);
    }

    /**
     * Intercept writeNbt to serialize skill levels as integers.
     */
    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void onWriteNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        // The original method writes unlockedSkills as a list of strings.
        // We need to replace it with a compound of integers.
        var levelsNbt = new NbtCompound();
        for (var entry : addon$skillLevels.entrySet()) {
            if (entry.getValue() > 0) {
                levelsNbt.putInt(entry.getKey(), entry.getValue());
            }
        }
        nbt.put("addon_skill_levels", levelsNbt);
    }

    /**
     * Intercept read (static factory) to load skill levels.
     * Note: This requires a different approach since read is static.
     * We'll use an @Inject at RETURN to populate our map after construction.
     */
    @Inject(method = "read", at = @At("RETURN"))
    private static void onRead(NbtCompound nbt, CallbackInfoReturnable<CategoryData> cir) {
        var categoryData = cir.getReturnValue();
        if (categoryData instanceof CategoryDataExtension ext) {
            // Load addon skill levels if present
            if (nbt.contains("addon_skill_levels", NbtElement.COMPOUND_TYPE)) {
                var levelsNbt = nbt.getCompound("addon_skill_levels");
                for (var key : levelsNbt.getKeys()) {
                    ext.addon$setSkillLevel(key, levelsNbt.getInt(key));
                }
            } else {
                // Backward compatibility: convert existing unlocked skills to level 1
                if (nbt.contains("unlocked_skills", NbtElement.LIST_TYPE)) {
                    var unlockedList = nbt.getList("unlocked_skills", NbtElement.STRING_TYPE);
                    for (var element : unlockedList) {
                        if (element instanceof NbtString nbtString) {
                            ext.addon$setSkillLevel(nbtString.asString(), 1);
                        }
                    }
                }
            }
        }
    }

    // ===== Helper Methods =====

    @Unique
    private int addon$getMaxLevelFromDefinition(CategoryConfig category, SkillConfig skill) {
        var definitionOpt = category.definitions().getById(skill.definitionId());
        if (definitionOpt.isEmpty()) {
            return 1;
        }
        var definition = definitionOpt.get();

        // Default max level is 1 (single unlock like original mod)
        // PerLevelRewardsReward can define higher max levels
        int maxLevel = 1;

        // Check for PerLevelRewardsReward which defines max level
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
