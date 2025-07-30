package net.puffish.skill_leveling.client.network.packets.in;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skill_leveling.network.InPacket;

public class NewPointInPacket implements InPacket {
	private final Identifier categoryId;

	private NewPointInPacket(Identifier categoryId) {
		this.categoryId = categoryId;
	}

	public static NewPointInPacket read(PacketByteBuf buf) {
		var categoryId = buf.readIdentifier();

		return new NewPointInPacket(
				categoryId
		);
	}

	public Identifier getCategoryId() {
		return categoryId;
	}
}
