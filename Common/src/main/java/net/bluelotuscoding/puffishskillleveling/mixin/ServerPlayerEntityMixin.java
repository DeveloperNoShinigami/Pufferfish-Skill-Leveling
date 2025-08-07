package net.bluelotuscoding.puffishskillleveling.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.bluelotuscoding.puffishskillleveling.api.SkillsAPI;
import net.bluelotuscoding.puffishskillleveling.experience.source.builtin.IncreaseStatExperienceSource;
import net.bluelotuscoding.puffishskillleveling.reward.builtin.AttributeReward;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void injectAtInit(CallbackInfo ci) {
		SkillsAPI.updateRewards((ServerPlayerEntity) (Object) this, AttributeReward.class);
	}

	@Inject(method = "increaseStat", at = @At("HEAD"))
	private void injectAtIncreaseStat(Stat<?> stat, int amount, CallbackInfo ci) {
		var player = (ServerPlayerEntity) (Object) this;
		SkillsAPI.updateExperienceSources(
				player,
				IncreaseStatExperienceSource.class,
				experienceSource -> experienceSource.getValue(player, stat, amount)
		);
	}
}
