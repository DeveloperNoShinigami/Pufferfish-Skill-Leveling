package net.bluelotuscoding.puffishskillleveling.server.network.packets.out;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.network.OutPacket;
import net.bluelotuscoding.puffishskillleveling.network.Packets;

public record NewPointOutPacket(Identifier categoryId) implements OutPacket {
	@Override
	public void write(PacketByteBuf buf) {
		buf.writeIdentifier(categoryId);
	}

	@Override
	public Identifier getId() {
		return Packets.NEW_POINT;
	}
}
