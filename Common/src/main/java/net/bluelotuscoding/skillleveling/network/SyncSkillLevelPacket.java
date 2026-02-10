package net.bluelotuscoding.skillleveling.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class SyncSkillLevelPacket {
    private final Identifier categoryId;
    private final String skillId;
    private final int baseLevel;
    private final int totalLevel;
    private final int maxLevel;
    private final int pointsPerLevel;
    private final String definitionId;
    private final boolean hidden;
    private final boolean toggle;
    private final int keybindSlot;

    public SyncSkillLevelPacket(Identifier categoryId, String skillId, int baseLevel, int totalLevel, int maxLevel,
            int pointsPerLevel, String definitionId, boolean hidden, boolean toggle, int keybindSlot) {
        this.categoryId = categoryId;
        this.skillId = skillId;
        this.baseLevel = baseLevel;
        this.totalLevel = totalLevel;
        this.maxLevel = maxLevel;
        this.pointsPerLevel = pointsPerLevel;
        this.definitionId = definitionId;
        this.hidden = hidden;
        this.toggle = toggle;
        this.keybindSlot = keybindSlot;
    }

    public void encode(PacketByteBuf buf) {
        buf.writeIdentifier(categoryId);
        buf.writeBoolean(skillId != null);
        if (skillId != null) {
            buf.writeString(skillId);
        }
        buf.writeInt(baseLevel);
        buf.writeInt(totalLevel);
        buf.writeInt(maxLevel);
        buf.writeInt(pointsPerLevel);
        buf.writeBoolean(definitionId != null);
        if (definitionId != null) {
            buf.writeString(definitionId);
        }
        buf.writeBoolean(hidden);
        buf.writeBoolean(toggle);
        buf.writeInt(keybindSlot);
    }

    public static SyncSkillLevelPacket decode(PacketByteBuf buf) {
        Identifier catId = buf.readIdentifier();
        String sId = null;
        if (buf.readBoolean()) {
            sId = buf.readString();
        }
        int baseLevel = buf.readInt();
        int totalLevel = buf.readInt();
        int maxLevel = buf.readInt();
        int pPerLevel = buf.readInt();
        String defId = null;
        if (buf.readBoolean()) {
            defId = buf.readString();
        }
        boolean hidden = buf.readBoolean();
        boolean toggle = buf.readBoolean();
        int keybindSlot = buf.readInt();
        return new SyncSkillLevelPacket(catId, sId, baseLevel, totalLevel, maxLevel, pPerLevel, defId,
                hidden, toggle, keybindSlot);
    }

    public void handleClient() {
        net.bluelotuscoding.skillleveling.client.ClientPacketHandler.handleSyncSkillLevel(categoryId, skillId,
                baseLevel,
                totalLevel, maxLevel, pointsPerLevel, definitionId, hidden, toggle, keybindSlot);
    }
}
