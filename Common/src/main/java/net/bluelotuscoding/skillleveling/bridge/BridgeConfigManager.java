package net.bluelotuscoding.skillleveling.bridge;

import java.io.File;
import net.bluelotuscoding.skillleveling.bridge.BridgeConfig;

public final class BridgeConfigManager {
    private static BridgeConfig config = new BridgeConfig();
    private static boolean loaded = false;

    private BridgeConfigManager() {
    }

    public static void load(File configDir) {
        // Purely in-memory now. Configuration is driven exclusively by Datapacks via BridgeDataLoader.
        logInfo("Epic Class bridge configuration initialized (Waiting for Datapacks...)");
        loaded = true;
    }

    public static void save() {
        // No-longer saving to local config file. Driven by Datapacks.
    }

    public static BridgeConfig getConfig() {
        return config;
    }

    public static void setConfig(BridgeConfig newConfig) {
        config = newConfig == null ? new BridgeConfig() : newConfig;
        config.applyMissingDefaults();
        loaded = true;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    private static void logInfo(String message) {
        net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER.info(message);
    }
}
