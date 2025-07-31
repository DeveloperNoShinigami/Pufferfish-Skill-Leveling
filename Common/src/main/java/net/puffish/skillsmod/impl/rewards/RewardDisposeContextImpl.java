package net.puffish.skillsmod.impl.rewards;

import net.minecraft.server.MinecraftServer;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.util.DisposeContext;

public record RewardDisposeContextImpl(DisposeContext context) implements RewardDisposeContext {
	@Override
	public MinecraftServer getServer() {
		return context.server();
	}
}
