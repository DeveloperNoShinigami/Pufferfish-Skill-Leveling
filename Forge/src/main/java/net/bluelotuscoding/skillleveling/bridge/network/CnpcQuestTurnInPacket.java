package net.bluelotuscoding.skillleveling.bridge.network;

import java.util.function.Supplier;
import net.bluelotuscoding.skillleveling.bridge.cnpc.runtime.CnpcQuestTurnInInvoker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.network.NetworkEvent;

public class CnpcQuestTurnInPacket {
    private final int questId;

    public CnpcQuestTurnInPacket(int questId) {
        this.questId = questId;
    }

    public static void encode(CnpcQuestTurnInPacket msg, PacketByteBuf buf) {
        buf.writeInt(msg.questId);
    }

    public static CnpcQuestTurnInPacket decode(PacketByteBuf buf) {
        return new CnpcQuestTurnInPacket(buf.readInt());
    }

    public static void handle(CnpcQuestTurnInPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.getSender();
            if (sender != null) {
                CnpcQuestTurnInInvoker.turnInQuest(sender, msg.questId);
            }
        });
        ctx.setPacketHandled(true);
    }
}
