package net.bluelotuscoding.puffishskillleveling.server.network.packets.out;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.network.OutPacket;
import net.bluelotuscoding.puffishskillleveling.network.Packets;
import net.bluelotuscoding.puffishskillleveling.util.ToastType;

public record ShowToastOutPacket(ToastType type) implements OutPacket {
	@Override
	public void write(PacketByteBuf buf) {
		buf.writeEnumConstant(type);
	}

	@Override
	public Identifier getId() {
		return Packets.SHOW_TOAST;
	}
}
