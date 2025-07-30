package net.puffish.skill_leveling.impl.experience.source;

import net.minecraft.server.MinecraftServer;
import net.puffish.skill_leveling.api.experience.source.ExperienceSourceDisposeContext;
import net.puffish.skill_leveling.util.DisposeContext;

public record ExperienceSourceDisposeContextImpl(DisposeContext context) implements ExperienceSourceDisposeContext {
	@Override
	public MinecraftServer getServer() {
		return context.server();
	}
}
