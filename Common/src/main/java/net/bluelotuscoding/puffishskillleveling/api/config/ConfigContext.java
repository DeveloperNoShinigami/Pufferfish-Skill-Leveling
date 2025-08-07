package net.bluelotuscoding.puffishskillleveling.api.config;

import net.minecraft.server.MinecraftServer;

public interface ConfigContext {
	MinecraftServer getServer();

	void emitWarning(String message);
}
