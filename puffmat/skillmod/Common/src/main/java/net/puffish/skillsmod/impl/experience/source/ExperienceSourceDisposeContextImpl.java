package net.puffish.skillsmod.impl.experience.source;

import net.minecraft.server.MinecraftServer;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceDisposeContext;
import net.puffish.skillsmod.util.DisposeContext;

public record ExperienceSourceDisposeContextImpl(DisposeContext context) implements ExperienceSourceDisposeContext {
	@Override
	public MinecraftServer getServer() {
		return context.server();
	}
}
