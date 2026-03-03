package net.bluelotuscoding.skillleveling.bridge.network;

// turbo-all
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraftforge.network.NetworkEvent;
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
        ctx.enqueueWork(() -> {
            net.minecraft.entity.player.PlayerEntity player = net.minecraft.client.MinecraftClient.getInstance().player;
            if (player != null && msg.nbt != null) {
                // Update the ecm_leveling tag in the player's persistent data
                player.getPersistentData().put("ecm_leveling", msg.nbt);
            }
        });
        ctx.setPacketHandled(true);
    }
}
