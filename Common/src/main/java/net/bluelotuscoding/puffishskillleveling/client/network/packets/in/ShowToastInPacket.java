package net.bluelotuscoding.puffishskillleveling.client.network.packets.in;

import net.minecraft.network.PacketByteBuf;
import net.bluelotuscoding.puffishskillleveling.network.InPacket;
import net.bluelotuscoding.puffishskillleveling.util.ToastType;

public class ShowToastInPacket implements InPacket {

	private final ToastType type;

	private ShowToastInPacket(ToastType type) {
		this.type = type;
	}

	public static ShowToastInPacket read(PacketByteBuf buf) {
		return new ShowToastInPacket(buf.readEnumConstant(ToastType.class));
	}

	public ToastType getToastType() {
		return type;
	}
}
