package net.bluelotuscoding.skillleveling.bridge.network;

// turbo-all
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.bluelotuscoding.skillleveling.bridge.forge.client.network.ForgeClientPacketHandlers;
import java.util.function.Supplier;

public class SyncCustomNbtPacket {
    public final NbtCompound nbt;

    public SyncCustomNbtPacket(NbtCompound nbt) {
        this.nbt = nbt;
    }

    public static void encode(SyncCustomNbtPacket msg, PacketByteBuf buf) {
        buf.writeNbt(msg.nbt);
    }

    public static SyncCustomNbtPacket decode(PacketByteBuf buf) {
        return new SyncCustomNbtPacket(buf.readNbt());
    }

    public static void handle(SyncCustomNbtPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> ForgeClientPacketHandlers.handleSyncCustomNbt(msg.nbt));
        }
        ctx.setPacketHandled(true);
    }
}
