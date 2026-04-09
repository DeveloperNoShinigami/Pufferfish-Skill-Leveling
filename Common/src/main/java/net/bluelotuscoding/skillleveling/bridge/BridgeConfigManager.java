package net.bluelotuscoding.skillleveling.bridge;

import java.io.File;
import java.lang.reflect.Method;
import net.bluelotuscoding.skillleveling.bridge.BridgeConfig;

public final class BridgeConfigManager {
    private static BridgeConfig config = new BridgeConfig();
    private static boolean loaded = false;

    private BridgeConfigManager() {
    }

    public static void load(File configDir) {
        // Purely in-memory now. Configuration is driven exclusively by Datapacks via BridgeDataLoader.
        syncLegacyEpicClassWeaponRestrictions(config);
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
        syncLegacyEpicClassWeaponRestrictions(config);
        loaded = true;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    private static void logInfo(String message) {
        net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER.info(message);
    }

    private static void syncLegacyEpicClassWeaponRestrictions(BridgeConfig bridgeConfig) {
        boolean enableLegacyRestrictions = bridgeConfig == null || !bridgeConfig.enableAutoClassWeaponRestrictions;
        try {
            Class<?> modSettingsClass = Class.forName("com.example.epicclassmod.data.ModSettings");
            Method setter = modSettingsClass.getMethod("setJobWeaponRestrEnabled", boolean.class);
            setter.invoke(null, enableLegacyRestrictions);
            logInfo("Bridge weapon restriction mode updated: bridgeAuto="
                    + !enableLegacyRestrictions + ", legacyEcm=" + enableLegacyRestrictions);
        } catch (Exception e) {
            logInfo("Epic Class legacy weapon restriction toggle unavailable: " + e.getMessage());
        }
    }
}
