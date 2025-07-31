package net.puffish.skillsmod.server.network.packets.out;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.network.OutPacket;
import net.puffish.skillsmod.network.Packets;

public record SkillUpdateOutPacket(Identifier categoryId, String skillId, boolean unlocked) implements OutPacket {
	@Override
	public void write(PacketByteBuf buf) {
		buf.writeIdentifier(categoryId);
		buf.writeString(skillId);
		buf.writeBoolean(unlocked);
	}

	@Override
	public Identifier getId() {
		return Packets.SKILL_UPDATE;
	}
}
