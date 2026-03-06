package net.bluelotuscoding.skillleveling.bridge.network;

import java.util.function.Supplier;
import net.bluelotuscoding.skillleveling.bridge.forge.client.screen.CustomClassSelectScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Packet sent from Server to Client to open the Advanced Class selection
 * screen,
 * filtered by the player's current class ID (the parent).
 */
public class OpenAdvanceClassScreenPacket {
    public final String parentClassId;

    public OpenAdvanceClassScreenPacket(String parentClassId) {
        this.parentClassId = parentClassId;
    }

    public static void encode(OpenAdvanceClassScreenPacket msg, PacketByteBuf buf) {
        buf.writeString(msg.parentClassId);
    }

    public static OpenAdvanceClassScreenPacket decode(PacketByteBuf buf) {
        return new OpenAdvanceClassScreenPacket(buf.readString());
    }

    public static void handle(OpenAdvanceClassScreenPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            // Must run on the client thread to open the screen safely
            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().setScreen(new CustomClassSelectScreen(msg.parentClassId));
            });
        });
        ctx.setPacketHandled(true);
    }
}
