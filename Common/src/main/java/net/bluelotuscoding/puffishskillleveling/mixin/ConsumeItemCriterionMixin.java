package net.bluelotuscoding.puffishskillleveling.mixin;

import net.minecraft.advancement.criterion.ConsumeItemCriterion;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.bluelotuscoding.puffishskillleveling.api.SkillsAPI;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.EatFoodExperienceSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConsumeItemCriterion.class)
public class ConsumeItemCriterionMixin {

	@Inject(method = "trigger", at = @At("HEAD"))
	private void injectAtTrigger(ServerPlayerEntity serverPlayer, ItemStack stack, CallbackInfo ci) {
		var food = stack.getItem().getFoodComponent();
		if (food != null) {
			SkillsAPI.updateExperienceSources(
					serverPlayer,
					EatFoodExperienceSource.class,
					experienceSource -> experienceSource.getValue(serverPlayer, stack)
			);
		}
	}
}
