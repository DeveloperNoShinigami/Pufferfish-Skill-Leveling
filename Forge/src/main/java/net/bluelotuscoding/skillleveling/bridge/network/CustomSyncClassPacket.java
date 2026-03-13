package net.bluelotuscoding.skillleveling.bridge.network;

import net.bluelotuscoding.skillleveling.client.ClientCustomClassState;
import net.minecraft.network.PacketByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Syncs the custom string-based class ID from server to client.
 */
public class CustomSyncClassPacket {
    public final UUID playerId;
    public final String customClassId;

    public CustomSyncClassPacket(UUID playerId, String customClassId) {
        this.playerId = playerId;
        this.customClassId = customClassId;
    }

    public static void encode(CustomSyncClassPacket msg, PacketByteBuf buf) {
        buf.writeUuid(msg.playerId);
        buf.writeString(msg.customClassId);
    }

    public static CustomSyncClassPacket decode(PacketByteBuf buf) {
        return new CustomSyncClassPacket(buf.readUuid(), buf.readString());
    }

    public static void handle(CustomSyncClassPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ClientCustomClassState.setCustomClass(msg.playerId, msg.customClassId);

            net.bluelotuscoding.skillleveling.bridge.forge.client.network.ForgeClientPacketHandlers.handleCustomSyncClass(msg.playerId, msg.customClassId);
        });
        ctx.setPacketHandled(true);
    }
}
