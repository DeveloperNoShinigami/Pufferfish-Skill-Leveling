package net.bluelotuscoding.puffishskillleveling.client.network.packets.in;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.network.InPacket;

public class ExtendedSkillUpdateInPacket implements InPacket {
	private final Identifier categoryId;
	private final String skillId;
        private final boolean unlocked;
        private final int level;

        private ExtendedSkillUpdateInPacket(Identifier categoryId, String skillId, boolean unlocked, int level) {
                this.categoryId = categoryId;
                this.skillId = skillId;
                this.unlocked = unlocked;
                this.level = level;
        }

        public static ExtendedSkillUpdateInPacket read(PacketByteBuf buf) {
                var categoryId = buf.readIdentifier();
                var skillId = buf.readString();
                var unlocked = buf.readBoolean();
                int level = unlocked ? 1 : 0;
                if (buf.readableBytes() >= 4) {
                        level = buf.readInt();
                }
                return new ExtendedSkillUpdateInPacket(
                                categoryId,
                                skillId,
                                unlocked,
                                level
                );
        }

	public Identifier getCategoryId() {
		return categoryId;
	}

	public String getSkillId() {
		return skillId;
	}

        public boolean isUnlocked() {
                return unlocked;
        }

        public int getLevel() {
                return level;
        }
}
