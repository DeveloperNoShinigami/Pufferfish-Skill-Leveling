package net.bluelotuscoding.puffishskillleveling.server.network;

import net.minecraft.server.network.ServerPlayerEntity;
import net.bluelotuscoding.puffishskillleveling.network.OutPacket;

public interface ServerPacketSender {
	void send(ServerPlayerEntity player, OutPacket packet);
}
