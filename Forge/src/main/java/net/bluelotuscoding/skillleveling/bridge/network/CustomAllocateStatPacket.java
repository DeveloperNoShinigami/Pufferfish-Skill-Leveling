package net.bluelotuscoding.skillleveling.bridge.network;

import net.bluelotuscoding.skillleveling.bridge.forge.EpicClassSyncHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class CustomAllocateStatPacket {
    public final String statId;
    public final int points;

    public CustomAllocateStatPacket(String statId, int points) {
        this.statId = statId;
        this.points = points;
    }

    public static void encode(CustomAllocateStatPacket msg, PacketByteBuf buf) {
        buf.writeString(msg.statId);
        buf.writeInt(msg.points);
    }

    public static CustomAllocateStatPacket decode(PacketByteBuf buf) {
        return new CustomAllocateStatPacket(buf.readString(), buf.readInt());
    }

    public static void handle(CustomAllocateStatPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player != null) {
                EpicClassSyncHelper.allocateStat(player, msg.statId, msg.points);
            }
        });
        ctx.setPacketHandled(true);
    }

    public void sendToServer() {
        net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler.CHANNEL.sendToServer(this);
    }
}
