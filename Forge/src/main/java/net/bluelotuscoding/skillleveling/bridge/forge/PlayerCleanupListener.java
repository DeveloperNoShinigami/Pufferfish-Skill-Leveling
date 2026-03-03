package net.bluelotuscoding.skillleveling.bridge.forge;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Listener to clean up player-specific tracking data when they disconnect.
 * Ensures no memory leaks from the PLAYER_ACTIVE_CATEGORY map in EpicClassBridge.
 */
@EventBusSubscriber(modid = SkillLevelingMod.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class PlayerCleanupListener {

    /**
     * Called when a player logs out. Removes their active category tracking.
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            // Remove the player's active category from the tracking map
            // This prevents memory leaks and ensures clean state when player rejoins
            EpicClassBridge.cleanupPlayerData(player);
        }
    }
}
