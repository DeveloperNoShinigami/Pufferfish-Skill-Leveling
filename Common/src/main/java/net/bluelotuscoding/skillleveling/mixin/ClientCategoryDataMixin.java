package net.bluelotuscoding.skillleveling.mixin;

import net.puffish.skillsmod.client.data.ClientCategoryData;
import net.puffish.skillsmod.client.config.skill.ClientSkillConfig;
import net.puffish.skillsmod.client.config.ClientCategoryConfig;
import net.puffish.skillsmod.api.Skill;
import net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
        int baseLevel = ClientSkillLevelStorage.getLevelByDefinitionId(defId);
        int bonus = ClientSkillLevelStorage.getEquipmentBonus(categoryId, skillId);
        int totalLevel = baseLevel + bonus;

        // Use the same robust max level logic as tooltips
        int maxLevel = Math.max(
                net.bluelotuscoding.skillleveling.client.ClientDescriptionStorage.getMaxLevel(defId),
                ClientSkillLevelStorage.getMaxLevel(categoryId, skillId));

        // 2. Decide State

        // IF THE SKILL IS AT MAX LEVEL - Always show as UNLOCKED (Green/Gold)
        if (totalLevel >= maxLevel && maxLevel > 0) {
            cir.setReturnValue(Skill.State.UNLOCKED);
            return;
        }

        // IF WE HAVE PROGRESS (either base level or gear)
        if (totalLevel > 0 || baseLevel > 0) {
            if (baseLevel < maxLevel) {
                // Not at max base level yet.
                // But should it look buyable? (Yellow border)
                // NO if it's loot-only.
                if (net.bluelotuscoding.skillleveling.client.ClientDescriptionStorage.isImbueOnly(defId) ||
                        net.bluelotuscoding.skillleveling.client.ClientDescriptionStorage.isTomeOnly(defId)) {
                    // Just show as active/unlocked, but not "purchasable"
                    cir.setReturnValue(Skill.State.UNLOCKED);
                    return;
                }

                // Show as purchasable (Yellow/White border)
                if (isAffordable(skill)) {
                    cir.setReturnValue(Skill.State.AFFORDABLE);
                } else {
                    cir.setReturnValue(Skill.State.AVAILABLE);
                }
            } else {
                // Base level is maxed, it's fully unlocked
                cir.setReturnValue(Skill.State.UNLOCKED);
            }
        }
    }

    @Shadow
    protected abstract boolean isAffordable(ClientSkillConfig skill);
}
