package net.bluelotuscoding.puffishskillleveling.server.setup;

import net.minecraft.server.network.ServerPlayerEntity;

public interface ServerPlatform {
	boolean isFakePlayer(ServerPlayerEntity player);
}
