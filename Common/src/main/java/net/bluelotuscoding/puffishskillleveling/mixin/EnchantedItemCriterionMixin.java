package net.bluelotuscoding.puffishskillleveling.mixin;

import net.minecraft.advancement.criterion.EnchantedItemCriterion;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.bluelotuscoding.puffishskillleveling.api.SkillsAPI;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.EnchantItemExperienceSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantedItemCriterion.class)
public class EnchantedItemCriterionMixin {

	@Inject(method = "trigger", at = @At("HEAD"))
	private void injectAtTrigger(ServerPlayerEntity serverPlayer, ItemStack stack, int levels, CallbackInfo ci) {
		SkillsAPI.updateExperienceSources(
				serverPlayer,
				EnchantItemExperienceSource.class,
				experienceSource -> experienceSource.getValue(serverPlayer, stack, levels)
		);
	}
}
