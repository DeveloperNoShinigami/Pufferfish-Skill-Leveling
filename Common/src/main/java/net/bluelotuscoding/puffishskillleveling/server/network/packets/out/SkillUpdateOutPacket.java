package net.bluelotuscoding.puffishskillleveling.server.network.packets.out;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.network.OutPacket;
import net.bluelotuscoding.puffishskillleveling.network.Packets;

public record SkillUpdateOutPacket(Identifier categoryId, String skillId, boolean unlocked, int level) implements OutPacket {
	@Override
	public void write(PacketByteBuf buf) {
		buf.writeIdentifier(categoryId);
		buf.writeString(skillId);
                buf.writeBoolean(unlocked);
                buf.writeInt(level);
        }

	@Override
	public Identifier getId() {
		return Packets.SKILL_UPDATE;
	}
}
