package net.puffish.skill_leveling.server.network;

import net.minecraft.server.network.ServerPlayerEntity;
import net.puffish.skill_leveling.network.OutPacket;

public interface ServerPacketSender {
	void send(ServerPlayerEntity player, OutPacket packet);
}
