package net.bluelotuscoding.skillleveling.mixin;

import net.puffish.skillsmod.client.data.ClientCategoryData;
import net.puffish.skillsmod.client.config.skill.ClientSkillConfig;
import net.puffish.skillsmod.client.config.ClientCategoryConfig;
import net.puffish.skillsmod.api.Skill;
import net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage;
import net.bluelotuscoding.skillleveling.client.ClientDescriptionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = ClientCategoryData.class, remap = false)
public abstract class ClientCategoryDataMixin {

    @Shadow
    private ClientCategoryConfig config;

    @Shadow
    private Map<String, Skill.State> skillStates;

    @Unique
    private final Map<String, Skill.State> addon$lastLoggedState = new java.util.HashMap<>();

    /**
     * Intercept getSpentPoints on the client to calculate correct spent points
     * for multi-level skills. This ensures the client's point display matches
     * the server's calculation in CategoryDataMixin.getSpentPoints.
     */
    @Inject(method = "getSpentPoints", at = @At("HEAD"), cancellable = true)
    private void onGetSpentPoints(CallbackInfoReturnable<Integer> cir) {
        if (config == null) {
            return;
        }

        String categoryId = config.id().toString();
        int totalSpent = 0;

        // Iterate through all skills and calculate their total cost based on level
        for (var skill : config.skills().values()) {
            String skillId = skill.id();

            // Get the definition for this skill via its definitionId
            var definition = config.definitions().get(skill.definitionId());
            int baseCost = (definition != null) ? definition.cost() : 1;

            if (ClientSkillLevelStorage.hasLevelInfo(categoryId, skillId)) {
                int level = ClientSkillLevelStorage.getBaseLevel(categoryId, skillId);

                // Use synced points_per_level if available, fallback to base cost
                int pointsPerLevel = ClientSkillLevelStorage.getPointsPerLevelByDefinitionId(skill.definitionId());
                if (pointsPerLevel <= 0) {
                    pointsPerLevel = baseCost;
                }

                totalSpent += level * pointsPerLevel;
            } else {
                // Fallback: check if skill is unlocked in base states (counts as 1 level)
                Skill.State state = skillStates.get(skillId);
                if (state == Skill.State.UNLOCKED) {
                    totalSpent += baseCost;
                }
            }
        }

        cir.setReturnValue(totalSpent);
    }

    /**
     * Intercept getSkillState on the client with same priority system as server:
     * 1. Prerequisites First: If custom prerequisites fail → LOCKED.
     * 2. Mastery Second: If max level reached → UNLOCKED (Gold).
     * 3. Toggle Logic Third:
     *    - Loot/Imbue toggle at Level 0 → LOCKED.
     *    - Standard Toggle / Learned Hybrid → AVAILABLE/AFFORDABLE (bypasses parents).
     * 4. Default: Normal progression logic.
     */
    @Inject(method = "getSkillState", at = @At("HEAD"), cancellable = true)
    private void onGetSkillState(ClientSkillConfig skill, CallbackInfoReturnable<Skill.State> cir) {
        if (config == null || skill == null) {
            return;
        }

        String categoryId = config.id().toString();
        String skillId = skill.id();
        String defId = skill.definitionId();

        int baseLevel = ClientSkillLevelStorage.getBaseLevel(categoryId, skillId);
        int totalLevel = ClientSkillLevelStorage.getLevel(categoryId, skillId);
        int maxLevel = Math.max(
                ClientDescriptionStorage.getMaxLevel(defId),
                ClientSkillLevelStorage.getMaxLevel(categoryId, skillId));

        // ── Priority 1: Prerequisites ──
        // If custom prerequisites fail, the skill is LOCKED (regardless of level/type).
        if (defId != null) {
            try {
                boolean prereqsMet = addon$checkClientPrerequisites(categoryId, defId);
                if (!prereqsMet) {
                    cir.setReturnValue(Skill.State.LOCKED);
                    return;
                }

                // If prerequisites ARE met and skill was hidden at level 0, reveal it
                boolean hidden = ClientSkillLevelStorage.isHidden(categoryId, skillId);
                if (hidden && totalLevel == 0) {
                    if (isAffordable(skill)) {
                        cir.setReturnValue(Skill.State.AFFORDABLE);
                    } else {
                        cir.setReturnValue(Skill.State.AVAILABLE);
                    }
                    return;
                }
            } catch (Exception e) {
                // Ignore errors during early initialization
            }
        }

        // ── Priority 2: Mastery ──
        // If max level is reached and NOT a toggle, the skill is UNLOCKED (Gold).
        // Toggle skills skip mastery — they remain togglable (handled in Priority 3).
        boolean isToggle = ClientSkillLevelStorage.isToggle(categoryId, skillId);
        if (maxLevel > 0 && totalLevel >= maxLevel && !isToggle) {
            cir.setReturnValue(Skill.State.UNLOCKED);
            return;
        }

        // ── Priority 3: Toggle Logic ──
        if (isToggle) {
            String lootMode = ClientDescriptionStorage.getLootMode(defId);
            if (lootMode.isEmpty()) {
                lootMode = ClientSkillLevelStorage.getLootModeByDefinitionId(defId);
            }
            boolean isLootLearned = lootMode != null && !lootMode.isEmpty();

            if (isLootLearned && totalLevel == 0) {
                // Loot/Imbue toggle at Level 0 → LOCKED
                cir.setReturnValue(Skill.State.LOCKED);
                return;
            }

            // Standard Toggle / Learned Hybrid → AVAILABLE/AFFORDABLE
            // Bypasses parent skill checks so toggles are always accessible
            if (isAffordable(skill)) {
                cir.setReturnValue(Skill.State.AFFORDABLE);
            } else {
                cir.setReturnValue(Skill.State.AVAILABLE);
            }
            return;
        }

        // ── Priority 4: Default ──
        // For skills with progression but not yet mastered:
        if (totalLevel > 0 || baseLevel > 0) {
            if (baseLevel < maxLevel) {
                if (isAffordable(skill)) {
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

    @Unique
    private boolean addon$checkClientPrerequisites(String categoryId, String definitionId) {
        var prereqs = ClientDescriptionStorage.getPrerequisites(definitionId);
        if (prereqs == null || prereqs.isEmpty()) {
            return true;
        }

        for (var req : prereqs) {
            // Support cross-category prerequisites
            String reqCategoryId = req.getCategoryId();
            if (reqCategoryId == null || reqCategoryId.isEmpty()) {
                reqCategoryId = categoryId; // Default to same category
            }

            // Use TOTAL level (base + equipment bonus) for prerequisite checking
            int currentLevel = ClientSkillLevelStorage.getLevel(reqCategoryId, req.getSkillId());

            if (currentLevel < req.getLevel()) {
                return false;
            }
        }
        return true;
    }

    @Shadow
    protected abstract boolean isAffordable(ClientSkillConfig skill);
}
