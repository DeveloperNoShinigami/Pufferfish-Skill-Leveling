package net.bluelotuscoding.skillleveling.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage;

public class SyncSkillLevelPacket {
    private final Identifier categoryId;
    private final String skillId;
    private final int level;
    private final int maxLevel;
    private final int pointsPerLevel;
    private final String definitionId;

    public SyncSkillLevelPacket(Identifier categoryId, String skillId, int level, int maxLevel, int pointsPerLevel) {
        this(categoryId, skillId, level, maxLevel, pointsPerLevel, null);
    }

    public SyncSkillLevelPacket(Identifier categoryId, String skillId, int level, int maxLevel, int pointsPerLevel,
            String definitionId) {
        this.categoryId = categoryId;
        this.skillId = skillId;
        this.level = level;
        this.maxLevel = maxLevel;
        this.pointsPerLevel = pointsPerLevel;
        this.definitionId = definitionId;
    }

    public void encode(PacketByteBuf buf) {
        buf.writeIdentifier(categoryId);
        buf.writeBoolean(skillId != null);
        if (skillId != null) {
            buf.writeString(skillId);
        }
        buf.writeInt(level);
        buf.writeInt(maxLevel);
        buf.writeInt(pointsPerLevel);
        buf.writeBoolean(definitionId != null);
        if (definitionId != null) {
            buf.writeString(definitionId);
        }
    }

    public static SyncSkillLevelPacket decode(PacketByteBuf buf) {
        Identifier catId = buf.readIdentifier();
        String sId = null;
        if (buf.readBoolean()) {
            sId = buf.readString();
        }
        int level = buf.readInt();
        int maxLevel = buf.readInt();
        int pPerLevel = buf.readInt();
        String defId = null;
        if (buf.readBoolean()) {
            defId = buf.readString();
        }
        return new SyncSkillLevelPacket(catId, sId, level, maxLevel, pPerLevel, defId);
    }

    public void handleClient() {
        ClientSkillLevelStorage.setLevel(categoryId.toString(), skillId, level, maxLevel, pointsPerLevel);
        // Register definition mapping if provided
        if (definitionId != null) {
            ClientSkillLevelStorage.registerDefinitionMapping(definitionId, categoryId.toString(), skillId);
        }
    }
}
