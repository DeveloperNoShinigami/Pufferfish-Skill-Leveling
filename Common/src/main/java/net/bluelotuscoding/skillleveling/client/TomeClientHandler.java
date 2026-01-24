package net.bluelotuscoding.skillleveling.client;

import net.bluelotuscoding.skillleveling.item.TomeItem;
import net.minecraft.util.Identifier;

/**
 * Client-side handler for Tome item usage.
 * Opens the Skills UI in the appropriate mode.
 */
public class TomeClientHandler {

    // Tracks the current selection mode for the Skills screen
    private static TomeItem.TomeType pendingTomeType = null;
    private static boolean inSelectionMode = false;

    // Platform-specific packet sender (set by FabricMain or ForgeMain client init)
    private static TomePacketSender packetSender = null;

    @FunctionalInterface
    public interface TomePacketSender {
        void send(Identifier categoryId, String skillId, TomeItem.TomeType tomeType);
    }

    /**
     * Set the packet sender (called by platform-specific client initializers).
     */
    public static void setPacketSender(TomePacketSender sender) {
        packetSender = sender;
    }

    /**
     * Called when a Tome is used on the client.
     * Opens the Skills screen in selection mode (for Clear Mind tomes)
     * or category selection mode (for Progression).
     */
    public static void openTomeUI(TomeItem.TomeType type) {
        pendingTomeType = type;
        inSelectionMode = true;
        // The Skills screen will be opened by the server via
        // SkillsAPI.openScreen(player)
    }

    /**
     * Check if we're currently in selection mode.
     */
    public static boolean isInSelectionMode() {
        return inSelectionMode;
    }

    /**
     * Get the pending tome type for the current selection.
     */
    public static TomeItem.TomeType getPendingTomeType() {
        return pendingTomeType;
    }

    /**
     * Clear the selection mode after use or cancellation.
     */
    public static void clearSelectionMode() {
        pendingTomeType = null;
        inSelectionMode = false;
    }

    /**
     * Check if the current tome requires skill selection.
     */
    public static boolean requiresSkillSelection() {
        return pendingTomeType == TomeItem.TomeType.CLEAR_MIND
                || pendingTomeType == TomeItem.TomeType.GREATER_CLEAR_MIND;
    }

    /**
     * Send the tome action packet to the server.
     */
    public static void sendTomeAction(Identifier categoryId, String skillId, TomeItem.TomeType tomeType) {
        if (packetSender != null) {
            packetSender.send(categoryId, skillId, tomeType);
        }
    }
}
