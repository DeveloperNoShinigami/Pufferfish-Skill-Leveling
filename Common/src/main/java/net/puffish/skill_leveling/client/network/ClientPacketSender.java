package net.puffish.skill_leveling.client.network;

import net.puffish.skill_leveling.network.OutPacket;

public interface ClientPacketSender {
	void send(OutPacket packet);
}
