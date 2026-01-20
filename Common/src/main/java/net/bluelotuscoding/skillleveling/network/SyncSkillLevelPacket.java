package net.bluelotuscoding.skillleveling.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.skillleveling.client.ClientSkillLevelStorage;

public class SyncSkillLevelPacket {
    private final Identifier categoryId;
    private final String skillId;
    private final int level;
    private final int maxLevel;
    private final String definitionId; // Added for client description mapping

    public SyncSkillLevelPacket(Identifier categoryId, String skillId, int level, int maxLevel) {
        this(categoryId, skillId, level, maxLevel, null);
    }

    public SyncSkillLevelPacket(Identifier categoryId, String skillId, int level, int maxLevel, String definitionId) {
        this.categoryId = categoryId;
        this.skillId = skillId;
        this.level = level;
        this.maxLevel = maxLevel;
        this.definitionId = definitionId;
    }

    public void encode(PacketByteBuf buf) {
        buf.writeIdentifier(categoryId);
        buf.writeString(skillId);
        buf.writeInt(level);
        buf.writeInt(maxLevel);
        buf.writeBoolean(definitionId != null);
        if (definitionId != null) {
            buf.writeString(definitionId);
        }
    }

    public static SyncSkillLevelPacket decode(PacketByteBuf buf) {
        Identifier catId = buf.readIdentifier();
        String skillId = buf.readString();
        int level = buf.readInt();
        int maxLevel = buf.readInt();
        String defId = null;
        if (buf.readBoolean()) {
            defId = buf.readString();
        }
        return new SyncSkillLevelPacket(catId, skillId, level, maxLevel, defId);
    }

    public void handleClient() {
        ClientSkillLevelStorage.setLevel(categoryId.toString(), skillId, level, maxLevel);
        // Register definition mapping if provided
        if (definitionId != null) {
            ClientSkillLevelStorage.registerDefinitionMapping(definitionId, categoryId.toString(), skillId);
        }
    }
}
