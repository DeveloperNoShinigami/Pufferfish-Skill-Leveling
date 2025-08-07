package net.bluelotuscoding.puffishskillleveling.util;

import net.minecraft.server.MinecraftServer;
import net.bluelotuscoding.puffishskillleveling.api.config.ConfigContext;

public record VersionedConfigContext(
		ConfigContext context,
		int version
) implements ConfigContext, VersionContext {

	@Override
	public MinecraftServer getServer() {
		return context.getServer();
	}

	@Override
	public void emitWarning(String message) {
		context.emitWarning(message);
	}

	@Override
	public int getVersion() {
		return version;
	}
}
