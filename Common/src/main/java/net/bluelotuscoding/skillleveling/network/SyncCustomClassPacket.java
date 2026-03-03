package net.bluelotuscoding.skillleveling.network;

import net.minecraft.network.PacketByteBuf;
import java.util.UUID;

/**
 * Syncs a custom class ID string from server to client.
 */
public class SyncCustomClassPacket {
    private final UUID playerId;
    private final String classId;

    public SyncCustomClassPacket(UUID playerId, String classId) {
        this.playerId = playerId;
        this.classId = classId;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(playerId);
        buf.writeString(classId);
    }

    public static SyncCustomClassPacket read(PacketByteBuf buf) {
        UUID playerId = buf.readUuid();
        String classId = buf.readString();
        return new SyncCustomClassPacket(playerId, classId);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getClassId() {
        return classId;
    }
}
