package net.puffish.skillsmod.client.network.packets.in;

import net.minecraft.network.PacketByteBuf;
import net.puffish.skillsmod.network.InPacket;
import net.puffish.skillsmod.util.ToastType;

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
