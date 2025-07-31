package net.puffish.skillsmod.api.reward;

import net.minecraft.server.network.ServerPlayerEntity;

public interface RewardUpdateContext {
	ServerPlayerEntity getPlayer();

	int getCount();

	boolean isAction();
}
