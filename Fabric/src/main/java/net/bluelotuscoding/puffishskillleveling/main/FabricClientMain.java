package net.bluelotuscoding.puffishskillleveling.main;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.client.SkillsClientMod;
import net.bluelotuscoding.puffishskillleveling.client.network.ClientPacketHandler;
import net.bluelotuscoding.puffishskillleveling.client.network.ClientPacketSender;
import net.bluelotuscoding.puffishskillleveling.client.setup.ClientRegistrar;
import net.bluelotuscoding.puffishskillleveling.network.InPacket;
import net.bluelotuscoding.puffishskillleveling.network.OutPacket;

import java.util.function.Function;

public class FabricClientMain implements ClientModInitializer {

        @Override
        public void onInitializeClient() {
                SkillsClientMod.setup(
                                new ClientRegistrarImpl(),
                                new ClientPacketSenderImpl()
                );
        }

	private static class ClientRegistrarImpl implements ClientRegistrar {
		@Override
		public <T extends InPacket> void registerInPacket(Identifier id, Function<PacketByteBuf, T> reader, ClientPacketHandler<T> handler) {
			ClientPlayNetworking.registerGlobalReceiver(
					id,
					(client, handler2, buf, responseSender) -> {
						var packet = reader.apply(buf);
						client.execute(() -> handler.handle(packet));
					}
			);
		}

		@Override
		public void registerOutPacket(Identifier id) { }
	}

        private static class ClientPacketSenderImpl implements ClientPacketSender {
                @Override
                public void send(OutPacket packet) {
                        var buf = new PacketByteBuf(Unpooled.buffer());
                        packet.write(buf);
                        ClientPlayNetworking.send(packet.getId(), buf);
                }
        }
}
