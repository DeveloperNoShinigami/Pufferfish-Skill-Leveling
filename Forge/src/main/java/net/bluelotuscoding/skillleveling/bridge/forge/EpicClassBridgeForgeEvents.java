package net.bluelotuscoding.skillleveling.bridge.forge;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EpicClassBridgeForgeEvents {
    private static final int CHECK_INTERVAL_TICKS = 20;
    private final Map<UUID, String> lastClass = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) {
            return;
        }
        if (player.getWorld().isClient) {
            return;
        }
        if (!EpicClassBridge.isEnabled() || !EpicClassBridge.shouldSyncOnLogin()) {
            return;
        }

        String className = EpicClassBridgeForgeAccess.getClassName(player);
        if (className == null) {
            return;
        }

        lastClass.put(player.getUuid(), className);
        var server = player.getServer();
        if (server != null) {
            server.execute(() -> EpicClassBridge.onClassChanged(player, className));
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            lastClass.remove(player.getUuid());
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayerEntity player)) {
            return;
        }
        if (player.getWorld().isClient) {
            return;
        }
        if (!EpicClassBridge.isEnabled()) {
            return;
        }
        long time = player.getWorld().getTime();
        if (time % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        String className = EpicClassBridgeForgeAccess.getClassName(player);
        if (className == null) {
            return;
        }

        String cached = lastClass.get(player.getUuid());
        if (cached == null) {
            lastClass.put(player.getUuid(), className);
            return;
        }

        if (!className.equals(cached)) {
            lastClass.put(player.getUuid(), className);
            EpicClassBridge.onClassChanged(player, className);
        }
    }
}
