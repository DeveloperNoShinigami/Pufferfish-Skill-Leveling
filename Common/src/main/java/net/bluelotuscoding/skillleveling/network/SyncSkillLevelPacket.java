package net.bluelotuscoding.skillleveling.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage;

public class SyncSkillLevelPacket {
    private final Identifier categoryId;
    private final String skillId;
    private final int level;
    private final int maxLevel;

    public SyncSkillLevelPacket(Identifier categoryId, String skillId, int level, int maxLevel) {
        this.categoryId = categoryId;
        this.skillId = skillId;
        this.level = level;
        this.maxLevel = maxLevel;
    }

    public void encode(PacketByteBuf buf) {
        buf.writeIdentifier(categoryId);
        buf.writeString(skillId);
        buf.writeInt(level);
        buf.writeInt(maxLevel);
    }

    public static SyncSkillLevelPacket decode(PacketByteBuf buf) {
        return new SyncSkillLevelPacket(
                buf.readIdentifier(),
                buf.readString(),
                buf.readInt(),
                buf.readInt());
    }

    public void handleClient() {
        ClientSkillLevelStorage.setLevel(categoryId.toString(), skillId, level, maxLevel);
    }
}
