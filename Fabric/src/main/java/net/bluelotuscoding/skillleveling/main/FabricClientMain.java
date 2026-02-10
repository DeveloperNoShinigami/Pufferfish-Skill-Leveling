package net.bluelotuscoding.skillleveling.main;

import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client main class for the Skill Leveling addon
 */
public class FabricClientMain implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// Client-side initialization
		net.bluelotuscoding.skillleveling.network.FabricNetworkHandler.initClient();

		// Mastery Keybinds
		net.bluelotuscoding.skillleveling.client.MasteryKeybinds.init();
		for (var key : net.bluelotuscoding.skillleveling.client.MasteryKeybinds.KEYBINDINGS) {
			net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(key);
		}

		// Client tick event for key polling
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			net.bluelotuscoding.skillleveling.client.MasteryKeybinds.onClientTick();
		});
	}
}