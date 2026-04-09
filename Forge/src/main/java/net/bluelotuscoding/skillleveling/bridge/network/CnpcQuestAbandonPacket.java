package net.bluelotuscoding.skillleveling.bridge.network;

import java.util.function.Supplier;
import net.bluelotuscoding.skillleveling.bridge.cnpc.runtime.CnpcRuntimeBridge;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.network.NetworkEvent;

public class CnpcQuestAbandonPacket {
    private final int questId;

    public CnpcQuestAbandonPacket(int questId) {
        this.questId = questId;
    }

    public static void encode(CnpcQuestAbandonPacket msg, PacketByteBuf buf) {
        buf.writeInt(msg.questId);
    }

    public static CnpcQuestAbandonPacket decode(PacketByteBuf buf) {
        return new CnpcQuestAbandonPacket(buf.readInt());
    }

    public static void handle(CnpcQuestAbandonPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.getSender();
            if (sender != null) {
                CnpcRuntimeBridge.dropAndRefresh(sender, msg.questId);
            }
        });
        ctx.setPacketHandled(true);
    }
}
