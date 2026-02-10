package net.bluelotuscoding.skillleveling.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class SyncToggleCooldownPacket {
    private final Identifier categoryId;
    private final String skillId;
    private final int cooldownTicks;

    public SyncToggleCooldownPacket(Identifier categoryId, String skillId, int cooldownTicks) {
        this.categoryId = categoryId;
        this.skillId = skillId;
        this.cooldownTicks = cooldownTicks;
    }

    public void encode(PacketByteBuf buf) {
        buf.writeIdentifier(categoryId);
        buf.writeString(skillId);
        buf.writeInt(cooldownTicks);
    }

    public static SyncToggleCooldownPacket decode(PacketByteBuf buf) {
        Identifier catId = buf.readIdentifier();
        String sId = buf.readString();
        int ticks = buf.readInt();
        return new SyncToggleCooldownPacket(catId, sId, ticks);
    }

    public void handleClient() {
        net.bluelotuscoding.skillleveling.client.ClientPacketHandler.handleSyncToggleCooldown(categoryId, skillId,
                cooldownTicks);
    }
}
