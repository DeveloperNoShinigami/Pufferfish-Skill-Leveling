package net.bluelotuscoding.puffishskillleveling.api.reward;

import net.minecraft.server.network.ServerPlayerEntity;

public interface RewardUpdateContext {
	ServerPlayerEntity getPlayer();

	int getCount();

	boolean isAction();
}
