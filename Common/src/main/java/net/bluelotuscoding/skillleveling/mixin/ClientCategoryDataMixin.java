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
     * Intercept getSkillState on the client to:
     * - Show UNLOCKED (highlighted) at max level
     * - Show AFFORDABLE/AVAILABLE below max level for re-purchase
     * - Ensure imbue_only/tome_only skills stay UNLOCKED and don't look buyable
     */
    @Inject(method = "getSkillState", at = @At("HEAD"), cancellable = true)
    private void onGetSkillState(ClientSkillConfig skill, CallbackInfoReturnable<Skill.State> cir) {
        if (config == null || skill == null) {
            return;
        }

        String categoryId = config.id().toString();
        String skillId = skill.id();
        String defId = skill.definitionId();

        // 1. Get current levels and max level using robust lookups
        int baseLevel = ClientSkillLevelStorage.getBaseLevel(categoryId, skillId);
        // SYNC FIX: Use the total level synced from server instead of recalculating
        // locally.
        // This ensures Curio bonuses (which aren't scanned in the basic EquipmentSlot
        // loop)
        // are correctly reflected in the GUI state (Golden/Unlocked).
        int totalLevel = ClientSkillLevelStorage.getLevel(categoryId, skillId);

        // Use the same robust max level logic as tooltips
        int maxLevel = Math.max(
                ClientDescriptionStorage.getMaxLevel(defId),
                ClientSkillLevelStorage.getMaxLevel(categoryId, skillId));

        // State will be determined below
        Skill.State determinedState = null;

        // DYNAMIC PREREQUISITE CHECK: Skills deactivate and lock/hide when
        // prerequisites are lost
        if (defId != null) {
            try {
                boolean prereqsMet = addon$checkClientPrerequisites(categoryId, defId);
                boolean hidden = ClientSkillLevelStorage.isHidden(categoryId, skillId);

                // If prerequisites are NOT met, keep skill locked/hidden
                if (!prereqsMet) {
                    determinedState = Skill.State.LOCKED;
                    cir.setReturnValue(determinedState);
                    return;
                }

                // If prerequisites ARE met but skill is level 0 and was hidden,
                // we MUST explicitly override the state to reveal it.
                if (hidden && totalLevel == 0) {
                    if (isAffordable(skill)) {
                        determinedState = Skill.State.AFFORDABLE;
                    } else {
                        determinedState = Skill.State.AVAILABLE;
                    }
                    cir.setReturnValue(determinedState);
                    return;
                }
            } catch (Exception e) {
                // Ignore errors during early initialization
            }
        }

        // 2. Decide State

        // Mastery Check: ONLY show as UNLOCKED (Golden) if TOTAL level reaches max
        if (maxLevel > 0 && totalLevel >= maxLevel) {
            determinedState = Skill.State.UNLOCKED;
            cir.setReturnValue(determinedState);
            return;
        }

        // Progression Check: If we are below max level
        if (totalLevel > 0 || baseLevel > 0 || !ClientSkillLevelStorage.isHidden(categoryId, skillId)) {
            // If baseLevel < maxLevel, it might be buyable or just an active loot skill
            if (baseLevel < maxLevel) {
                // If it's a loot-only skill, it shouldn't look "buyable" (AFFORDABLE/AVAILABLE)
                // in a way that suggests spending points if it's not possible.
                if (isAffordable(skill)) {
                    determinedState = Skill.State.AFFORDABLE;
                } else {
                    determinedState = Skill.State.AVAILABLE;
                }
                cir.setReturnValue(determinedState);
                return;
            } else {
                // Base at max, but total might not be (penalty) - show UNLOCKED
                determinedState = Skill.State.UNLOCKED;
                cir.setReturnValue(determinedState);
                return;
            }
        }
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
