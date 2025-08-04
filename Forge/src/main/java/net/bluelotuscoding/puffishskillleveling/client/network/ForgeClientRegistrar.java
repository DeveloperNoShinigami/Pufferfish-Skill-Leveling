package net.bluelotuscoding.puffishskillleveling.client.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.client.network.ClientPacketHandler;
import net.puffish.skillsmod.client.setup.ClientRegistrar;
import net.puffish.skillsmod.network.InPacket;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.event.EventNetworkChannel;

import java.util.function.Function;

/**
 * Registers packet handlers on the Forge networking system for the
 * Skills API. Incoming packets are decoded using the provided reader
 * and forwarded to the supplied handler.
 */
public class ForgeClientRegistrar implements ClientRegistrar {
    @Override
    public <T extends InPacket> void registerInPacket(ResourceLocation id,
            Function<FriendlyByteBuf, T> reader,
            ClientPacketHandler<T> handler) {
        EventNetworkChannel channel = NetworkRegistry.newEventChannel(id, () -> "1.0", s -> true, s -> true);
        channel.addListener((NetworkEvent event) -> {
            NetworkEvent.Context ctx = event.getSource().get();
            if (ctx.getPacketHandled()) {
                return;
            }
            if (event instanceof NetworkEvent.ServerCustomPayloadEvent payload) {
                T packet = reader.apply(payload.getPayload());
                ctx.enqueueWork(() -> handler.handle(packet));
                ctx.setPacketHandled(true);
            }
        });
    }

    @Override
    public void registerOutPacket(ResourceLocation id) {
        // No registration required for client-to-server packets in Forge
    }
}
