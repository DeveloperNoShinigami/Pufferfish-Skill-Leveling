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

        if (ClientSkillLevelStorage.hasLevelInfo(categoryId, skillId)) {
            int currentLevel = ClientSkillLevelStorage.getLevel(categoryId, skillId);
            int maxLevel = ClientSkillLevelStorage.getMaxLevel(categoryId, skillId);

            if (currentLevel >= maxLevel && currentLevel > 0) {
                // AT MAX LEVEL - show as UNLOCKED (highlighted/completed)
                cir.setReturnValue(Skill.State.UNLOCKED);
            } else if (currentLevel > 0 && currentLevel < maxLevel) {
                // Not at max level yet - show as purchasable
                Skill.State originalState = skillStates.get(skillId);
                if (originalState == Skill.State.UNLOCKED) {
                    // It IS unlocked in Pufferfish, but we want it to look buyable in our UI
                    if (isAffordable(skill)) {
                        cir.setReturnValue(Skill.State.AFFORDABLE);
                    } else {
                        cir.setReturnValue(Skill.State.AVAILABLE);
                    }
                }
            }
        }
    }

    @Shadow
    protected abstract boolean isAffordable(ClientSkillConfig skill);
}
