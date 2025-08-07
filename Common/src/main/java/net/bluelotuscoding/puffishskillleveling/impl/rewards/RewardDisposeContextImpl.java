package net.bluelotuscoding.puffishskillleveling.impl.rewards;

import net.minecraft.server.MinecraftServer;
import net.bluelotuscoding.puffishskillleveling.api.reward.RewardDisposeContext;
import net.bluelotuscoding.puffishskillleveling.util.DisposeContext;

public record RewardDisposeContextImpl(DisposeContext context) implements RewardDisposeContext {
	@Override
	public MinecraftServer getServer() {
		return context.server();
	}
}
