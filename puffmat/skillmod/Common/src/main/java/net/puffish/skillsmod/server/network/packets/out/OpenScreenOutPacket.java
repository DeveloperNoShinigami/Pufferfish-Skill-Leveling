package net.puffish.skillsmod.server.network.packets.out;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.network.OutPacket;
import net.puffish.skillsmod.network.Packets;

import java.util.Optional;

public record OpenScreenOutPacket(Optional<Identifier> category) implements OutPacket {
	@Override
	public void write(PacketByteBuf buf) {
		buf.writeOptional(category, PacketByteBuf::writeIdentifier);
	}

	@Override
	public Identifier getId() {
		return Packets.OPEN_SCREEN;
	}
}
