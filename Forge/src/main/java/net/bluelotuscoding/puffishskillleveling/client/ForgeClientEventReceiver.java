package net.bluelotuscoding.puffishskillleveling.client;

import net.puffish.skillsmod.client.event.ClientEventListener;
import net.puffish.skillsmod.client.event.ClientEventReceiver;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Bridges Forge's event bus to the Skills API's client event system.
 * Currently only the player join event is forwarded.
 */
public class ForgeClientEventReceiver implements ClientEventReceiver {
    private final List<ClientEventListener> listeners = new ArrayList<>();

    public ForgeClientEventReceiver() {
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
            for (ClientEventListener listener : listeners) {
                listener.onPlayerJoin();
            }
        });
    }

    @Override
    public void registerListener(ClientEventListener listener) {
        listeners.add(listener);
    }
}
