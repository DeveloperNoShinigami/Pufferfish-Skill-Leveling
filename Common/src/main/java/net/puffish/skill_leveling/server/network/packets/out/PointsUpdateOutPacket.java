package net.puffish.skill_leveling.server.network.packets.out;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skill_leveling.network.OutPacket;
import net.puffish.skill_leveling.network.Packets;

public record PointsUpdateOutPacket(Identifier categoryId, int spentPoints, int earnedPoints) implements OutPacket {
	@Override
	public void write(PacketByteBuf buf) {
		buf.writeIdentifier(categoryId);
		buf.writeInt(spentPoints);
		buf.writeInt(earnedPoints);
	}

	@Override
	public Identifier getId() {
		return Packets.POINTS_UPDATE;
	}
}
