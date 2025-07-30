package net.puffish.skill_leveling.client.network.packets.in;

import net.minecraft.network.PacketByteBuf;
import net.puffish.skill_leveling.network.InPacket;
import net.puffish.skill_leveling.util.ToastType;

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
