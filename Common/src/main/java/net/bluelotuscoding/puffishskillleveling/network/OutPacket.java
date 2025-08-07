package net.bluelotuscoding.puffishskillleveling.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public interface OutPacket {
	Identifier getId();

	void write(PacketByteBuf buf);
}
