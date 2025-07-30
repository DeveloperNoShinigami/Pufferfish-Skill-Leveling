package net.puffish.skill_leveling.server.network.packets.out;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skill_leveling.network.OutPacket;
import net.puffish.skill_leveling.network.Packets;
import net.puffish.skill_leveling.util.ToastType;

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
