package net.bluelotuscoding.puffishskillleveling.impl.config;

import net.minecraft.server.MinecraftServer;
import net.bluelotuscoding.puffishskillleveling.api.config.ConfigContext;

import java.util.ArrayList;
import java.util.List;

public record ConfigContextImpl(MinecraftServer server, List<String> warnings) implements ConfigContext {
	public ConfigContextImpl(MinecraftServer server) {
		this(server, new ArrayList<>());
	}

	@Override
	public MinecraftServer getServer() {
		return server;
	}

	@Override
	public void emitWarning(String message) {
		warnings.add(message);
	}
}
