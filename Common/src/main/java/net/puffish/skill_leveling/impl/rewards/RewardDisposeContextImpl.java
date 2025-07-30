package net.puffish.skill_leveling.impl.rewards;

import net.minecraft.server.MinecraftServer;
import net.puffish.skill_leveling.api.reward.RewardDisposeContext;
import net.puffish.skill_leveling.util.DisposeContext;

public record RewardDisposeContextImpl(DisposeContext context) implements RewardDisposeContext {
	@Override
	public MinecraftServer getServer() {
		return context.server();
	}
}
