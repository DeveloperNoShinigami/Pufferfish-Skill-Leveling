package net.puffish.skillsmod.client.network.packets.in;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.network.InPacket;

import java.util.Optional;

public class OpenScreenInPacket implements InPacket {
	private final Optional<Identifier> categoryId;

	private OpenScreenInPacket(Optional<Identifier> categoryId) {
		this.categoryId = categoryId;
	}

	public static OpenScreenInPacket read(PacketByteBuf buf) {
		return new OpenScreenInPacket(buf.readOptional(PacketByteBuf::readIdentifier));
	}

	public Optional<Identifier> getCategoryId() {
		return categoryId;
	}
}
