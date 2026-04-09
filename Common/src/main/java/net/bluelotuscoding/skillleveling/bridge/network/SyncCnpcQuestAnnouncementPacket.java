package net.bluelotuscoding.skillleveling.bridge.network;

import net.minecraft.network.PacketByteBuf;

public class SyncCnpcQuestAnnouncementPacket {
    private final long sequence;
    private final String questTitle;
    private final boolean completed;

    public SyncCnpcQuestAnnouncementPacket(long sequence, String questTitle, boolean completed) {
        this.sequence = sequence;
        this.questTitle = questTitle == null ? "" : questTitle;
        this.completed = completed;
    }

    public static void encode(SyncCnpcQuestAnnouncementPacket msg, PacketByteBuf buf) {
        buf.writeLong(msg.sequence);
        buf.writeString(msg.questTitle);
        buf.writeBoolean(msg.completed);
    }

    public static SyncCnpcQuestAnnouncementPacket decode(PacketByteBuf buf) {
        return new SyncCnpcQuestAnnouncementPacket(buf.readLong(), buf.readString(), buf.readBoolean());
    }

    public long getSequence() {
        return sequence;
    }

    public String getQuestTitle() {
        return questTitle;
    }

    public boolean isCompleted() {
        return completed;
    }
}
