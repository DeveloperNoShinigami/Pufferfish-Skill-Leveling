package net.puffish.skillsmod.client.addon;

/**
 * Handles client-side registration for the Skill Leveling addon.
 * <p>
 * This class provides hooks to register any additional packet handlers
 * and configuration required by the addon without modifying the base
 * mod's client bootstrap classes.
 */
public final class SkillLevelingAddonClient {

    private SkillLevelingAddonClient() {
    }

    /**
     * Registers addon specific networking and configuration.
     *
     * <p>Currently this method is a placeholder and performs no actions,
     * but it allows future work to plug in custom behaviour in a single
     * location.
     */
    public static void setup() {
        registerPackets();
        registerConfigs();
    }

    private static void registerPackets() {
        // Addon packet handlers can be registered here in the future.
    }

    private static void registerConfigs() {
        // Addon client configuration can be registered here in the future.
    }
}

