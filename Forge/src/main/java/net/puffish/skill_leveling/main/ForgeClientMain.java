package net.puffish.skill_leveling.main;

import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.puffish.skill_leveling.client.SkillsClientMod;
import net.puffish.skill_leveling.client.event.ClientEventListener;
import net.puffish.skill_leveling.client.event.ClientEventReceiver;
import net.puffish.skill_leveling.client.keybinding.KeyBindingHandler;
import net.puffish.skill_leveling.client.keybinding.KeyBindingReceiver;
import net.puffish.skill_leveling.client.network.ClientPacketHandler;
import net.puffish.skill_leveling.client.network.ClientPacketSender;
import net.puffish.skill_leveling.client.setup.ClientRegistrar;
import net.puffish.skill_leveling.network.InPacket;
import net.puffish.skill_leveling.network.OutPacket;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ForgeClientMain {
	private final List<ClientEventListener> clientListeners = new ArrayList<>();
	private final List<KeyBindingWithHandler> keyBindings = new ArrayList<>();

	public ForgeClientMain() {
		var forgeEventBus = MinecraftForge.EVENT_BUS;
		forgeEventBus.addListener(this::onPlayerLoggedIn);
		forgeEventBus.addListener(this::onInputKey);

		SkillsClientMod.setup(
				new ClientRegistrarImpl(),
				new ClientEventReceiverImpl(),
				new KeyBindingReceiverImpl(),
				new ClientPacketSenderImpl()
		);
	}

	private void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
		for (var listener : clientListeners) {
			listener.onPlayerJoin();
		}
	}

	private void onInputKey(InputEvent.Key event) {
		for (var keyBinding : keyBindings) {
			if (keyBinding.keyBinding.wasPressed()) {
				keyBinding.handler.handle();
			}
		}
	}

	private record KeyBindingWithHandler(KeyBinding keyBinding, KeyBindingHandler handler) {
	}

	private static class ClientRegistrarImpl implements ClientRegistrar {
		@Override
		public <T extends InPacket> void registerInPacket(Identifier identifier, Function<PacketByteBuf, T> reader, ClientPacketHandler<T> handler) {
			var channel = NetworkRegistry.newEventChannel(
					identifier,
					() -> "1",
					version -> true,
					version -> true
			);
			channel.addListener(networkEvent -> {
				var context = networkEvent.getSource().get();
				if (context.getPacketHandled()) {
					return;
				}
				if (networkEvent instanceof NetworkEvent.ServerCustomPayloadEvent serverNetworkEvent) {
					var packet = reader.apply(serverNetworkEvent.getPayload());
					context.enqueueWork(() -> handler.handle(packet));
					context.setPacketHandled(true);
				}
			});
		}

		@Override
		public void registerOutPacket(Identifier id) { }
	}

	private class ClientEventReceiverImpl implements ClientEventReceiver {
		@Override
		public void registerListener(ClientEventListener eventListener) {
			clientListeners.add(eventListener);
		}
	}

	private class KeyBindingReceiverImpl implements KeyBindingReceiver {
		@Override
		public void registerKeyBinding(KeyBinding keyBinding, KeyBindingHandler handler) {
			keyBindings.add(new KeyBindingWithHandler(keyBinding, handler));
			var options = MinecraftClient.getInstance().options;
			options.allKeys = ArrayUtils.add(options.allKeys, keyBinding);
		}
	}

	private static class ClientPacketSenderImpl implements ClientPacketSender {
		@Override
		public void send(OutPacket packet) {
			var buf = new PacketByteBuf(Unpooled.buffer());
			packet.write(buf);
			Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler())
					.sendPacket(new CustomPayloadC2SPacket(packet.getId(), buf));
		}
	}
}
