package net.bluelotuscoding.skillleveling.bridge.forge;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Handles Epic Class Bridge integration for class changes and player tracking.
 * 
 * Note: Experience syncing is now handled by PlayerLevelDataMixin which
 * intercepts
 * the addXp() method directly for real-time event-based syncing.
 */
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

        String className = net.bluelotuscoding.skillleveling.bridge.data.CustomClassData.getCustomClass(player);
        if (className != null) {
            lastClass.put(player.getUuid(), className);

            var server = player.getServer();
            if (server != null) {
                // Initial sync on login. We use a slight delay or execute on next tick
                // to ensure Pufferfish categories are loaded and ready.
                server.execute(() -> {
                    EpicClassBridge.onClassChanged(player, className);
                    EpicClassBridge.syncOnPlayerLogin(player);
                });
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            UUID playerUuid = player.getUuid();
            lastClass.remove(playerUuid);
            EpicClassBridge.cleanupPlayerData(player);
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

        String className = net.bluelotuscoding.skillleveling.bridge.data.CustomClassData.getCustomClass(player);
        if (className == null || "epic_classes:none".equals(className)) {
            return;
        }

        UUID playerUuid = player.getUuid();

        // Check if class changed or if this is the first detection
        String cached = lastClass.get(playerUuid);
        if (cached == null || !className.equals(cached)) {
            lastClass.put(playerUuid, className);
            EpicClassBridge.onClassChanged(player, className);
        }
    }
}
