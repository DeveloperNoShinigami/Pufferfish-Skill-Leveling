package net.bluelotuscoding.skillleveling.bridge.network;

import net.minecraft.network.PacketByteBuf;

public class SyncCnpcNpcRolePacket {
    private final int entityId;
    private final String jobMasterClassId;
    private final String questNpcRoleId;

    public SyncCnpcNpcRolePacket(int entityId, String jobMasterClassId, String questNpcRoleId) {
        this.entityId = entityId;
        this.jobMasterClassId = normalize(jobMasterClassId);
        this.questNpcRoleId = normalize(questNpcRoleId);
    }

    public static void encode(SyncCnpcNpcRolePacket msg, PacketByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeString(msg.jobMasterClassId == null ? "" : msg.jobMasterClassId);
        buf.writeString(msg.questNpcRoleId == null ? "" : msg.questNpcRoleId);
    }

    public static SyncCnpcNpcRolePacket decode(PacketByteBuf buf) {
        return new SyncCnpcNpcRolePacket(buf.readInt(), buf.readString(), buf.readString());
    }

    public int getEntityId() {
        return entityId;
    }

    public String getJobMasterClassId() {
        return jobMasterClassId;
    }

    public String getQuestNpcRoleId() {
        return questNpcRoleId;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
