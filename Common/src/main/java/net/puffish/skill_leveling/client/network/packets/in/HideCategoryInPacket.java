package net.puffish.skill_leveling.client.network.packets.in;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skill_leveling.network.InPacket;

public class HideCategoryInPacket implements InPacket {
	private final Identifier categoryId;

	private HideCategoryInPacket(Identifier categoryId) {
		this.categoryId = categoryId;
	}

	public static HideCategoryInPacket read(PacketByteBuf buf) {
		var categoryId = buf.readIdentifier();

		return new HideCategoryInPacket(
				categoryId
		);
	}

	public Identifier getCategoryId() {
		return categoryId;
	}
}
