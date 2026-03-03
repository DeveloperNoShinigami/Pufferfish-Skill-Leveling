package net.bluelotuscoding.skillleveling.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores custom class ID strings on the client side.
 */
public final class ClientCustomClassState {
    private static final Map<UUID, String> PLAYER_CUSTOM_CLASSES = new HashMap<>();

    private ClientCustomClassState() {
    }

    public static void setCustomClass(UUID playerId, String classId) {
        PLAYER_CUSTOM_CLASSES.put(playerId, classId);
    }

    public static String getCustomClass(UUID playerId) {
        return PLAYER_CUSTOM_CLASSES.getOrDefault(playerId, "epic_classes:none");
    }

    /**
     * Resets all cached custom class data (e.g., on world leave).
     */
    public static void reset() {
        PLAYER_CUSTOM_CLASSES.clear();
    }
}
