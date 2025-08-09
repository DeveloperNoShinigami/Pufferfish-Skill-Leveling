package net.bluelotuscoding.puffishskillleveling.client.network;

import net.bluelotuscoding.puffishskillleveling.network.OutPacket;

public interface ClientPacketSender {
	void send(OutPacket packet);
}
