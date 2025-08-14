package com.developernoshingami.pufferfish.skillleveling.main;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.puffish.skillsmod.network.InPacket;
import net.puffish.skillsmod.network.OutPacket;
import net.puffish.skillsmod.server.event.ServerEventListener;
import net.puffish.skillsmod.server.event.ServerEventReceiver;
import net.puffish.skillsmod.server.network.ServerPacketHandler;
import net.puffish.skillsmod.server.network.ServerPacketSender;
import net.puffish.skillsmod.server.setup.ServerPlatform;
import net.puffish.skillsmod.server.setup.ServerRegistrar;
import com.developernoshingami.pufferfish.skillleveling.SkillLevelingMod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Forge main class for the Skill Leveling addon
 */
@Mod(SkillLevelingMod.MOD_ID)
public class ForgeMain {

	private final List<ServerEventListener> serverListeners = new ArrayList<>();

	public ForgeMain() {
		MinecraftForge.EVENT_BUS.register(this);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
	}

	private void setup(FMLCommonSetupEvent event) {
		SkillLevelingMod.setup(
				ServerLifecycleHooks.getCurrentServer() != null ? 
					ServerLifecycleHooks.getCurrentServer().getRunDirectory().toPath().resolve("config") :
					null,
				new ServerRegistrarImpl(),
				new ServerEventReceiverImpl(),
				new ServerPacketSenderImpl(),
				new ServerPlatformImpl()
		);
	}

	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		for (var listener : serverListeners) {
			listener.onServerStarting(event.getServer());
		}
	}

	@SubscribeEvent
	public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayerEntity player) {
			for (var listener : serverListeners) {
				listener.onPlayerJoin(player);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayerEntity player) {
			for (var listener : serverListeners) {
				listener.onPlayerLeave(player);
			}
		}
	}

	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		var dispatcher = event.getDispatcher();
		for (var listener : serverListeners) {
			listener.onCommandsRegister(dispatcher);
		}
	}

	private class ServerRegistrarImpl implements ServerRegistrar {
		@Override
		public <V, T extends V> void register(Registry<V> registry, Identifier id, T entry) {
			// Forge registration would happen here
		}

		@Override
		public <T extends InPacket> void registerInPacket(Identifier identifier, Function<PacketByteBuf, T> reader, ServerPacketHandler<T> handler) {
			// Forge packet registration would happen here
		}

		@Override
		public void registerOutPacket(Identifier id) {
			// Forge doesn't require explicit outbound packet registration
		}
	}

	private class ServerEventReceiverImpl implements ServerEventReceiver {
		@Override
		public void registerListener(ServerEventListener eventListener) {
			serverListeners.add(eventListener);
		}
	}

	private class ServerPacketSenderImpl implements ServerPacketSender {
		@Override
		public void send(ServerPlayerEntity player, OutPacket packet) {
			// Note: This assumes the core mod handles packet sending
			// In a real addon, you might need to coordinate with the core mod
		}
	}

	private class ServerPlatformImpl implements ServerPlatform {
		@Override
		public boolean isFakePlayer(ServerPlayerEntity player) {
			// Use Forge's fake player detection
			return false; // Simplified implementation
		}
	}
}