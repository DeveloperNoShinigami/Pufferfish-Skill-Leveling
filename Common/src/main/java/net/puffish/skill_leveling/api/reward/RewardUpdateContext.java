package net.puffish.skill_leveling.api.reward;

import net.minecraft.server.network.ServerPlayerEntity;

public interface RewardUpdateContext {
	ServerPlayerEntity getPlayer();

	int getCount();

	boolean isAction();
}
