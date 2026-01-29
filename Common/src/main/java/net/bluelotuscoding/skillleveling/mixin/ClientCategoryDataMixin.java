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
     */
    @Inject(method = "getSkillState", at = @At("HEAD"), cancellable = true)
    private void onGetSkillState(ClientSkillConfig skill, CallbackInfoReturnable<Skill.State> cir) {
        if (config == null || skill == null) {
            return;
        }

        String categoryId = config.id().toString();
        String skillId = skill.id();

        // 1. Get current levels and max level
        int bonus = ClientSkillLevelStorage.getEquipmentBonus(categoryId, skillId);
        int baseLevel = ClientSkillLevelStorage.hasLevelInfo(categoryId, skillId)
                ? ClientSkillLevelStorage.getBaseLevel(categoryId, skillId)
                : 0;

        // Use ClientDescriptionStorage as the source of truth for max level
        // as it's populated for all skills regardless of player progress.
        int maxLevel = net.bluelotuscoding.skillleveling.client.ClientDescriptionStorage
                .getMaxLevel(skill.definitionId());
        int totalLevel = baseLevel + bonus;

        // 2. Decide State
        if (totalLevel >= maxLevel && maxLevel > 0) {
            // AT MAX LEVEL - show as UNLOCKED (highlighted/completed)
            cir.setReturnValue(Skill.State.UNLOCKED);
            return;
        }

        // 3. If not maxed, but we have some progress
        if (totalLevel > 0 || ClientSkillLevelStorage.hasLevelInfo(categoryId, skillId)) {
            if (baseLevel < maxLevel) {
                // Not at max base level yet - show as purchasable
                if (isAffordable(skill)) {
                    cir.setReturnValue(Skill.State.AFFORDABLE);
                } else {
                    cir.setReturnValue(Skill.State.AVAILABLE);
                }
            } else {
                // Base level is maxed, but total isn't (maybe gear penalty? or just maxed)
                cir.setReturnValue(Skill.State.UNLOCKED);
            }
        }
    }

    @Shadow
    protected abstract boolean isAffordable(ClientSkillConfig skill);
}
