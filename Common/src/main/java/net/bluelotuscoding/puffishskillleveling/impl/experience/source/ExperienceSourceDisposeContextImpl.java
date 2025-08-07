package net.bluelotuscoding.puffishskillleveling.impl.experience.source;

import net.minecraft.server.MinecraftServer;
import net.bluelotuscoding.puffishskillleveling.api.experience.source.ExperienceSourceDisposeContext;
import net.bluelotuscoding.puffishskillleveling.util.DisposeContext;

public record ExperienceSourceDisposeContextImpl(DisposeContext context) implements ExperienceSourceDisposeContext {
	@Override
	public MinecraftServer getServer() {
		return context.server();
	}
}
