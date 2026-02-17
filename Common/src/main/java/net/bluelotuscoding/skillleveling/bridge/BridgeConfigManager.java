package net.bluelotuscoding.skillleveling.bridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

public final class BridgeConfigManager {
    private static final String FILE_NAME = "pufferfish_skills_bridge.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static BridgeConfig config = new BridgeConfig();
    private static File configFile;
    private static boolean loaded = false;

    private BridgeConfigManager() {
    }

    public static void load(File configDir) {
        if (configDir == null) {
            return;
        }

        if (!configDir.exists() && !configDir.mkdirs()) {
            logWarn("Bridge config folder could not be created: " + configDir.getAbsolutePath());
            return;
        }

        configFile = new File(configDir, FILE_NAME);
        if (configFile.exists()) {
            try (Reader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
                BridgeConfig loadedConfig = GSON.fromJson(reader, BridgeConfig.class);
                if (loadedConfig != null) {
                    config = loadedConfig;
                }
            } catch (Exception e) {
                logWarn("Failed to read bridge config, using defaults: " + e.getMessage());
            }
        } else {
            save();
        }

        if (config.classToCategoryMap == null) {
            config.classToCategoryMap = new java.util.HashMap<>();
        }

        loaded = true;
        logInfo("Epic Class bridge config loaded (" + config.classToCategoryMap.size() + " mappings)");
    }

    public static void save() {
        if (configFile == null) {
            return;
        }

        try (Writer writer = Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        } catch (Exception e) {
            logWarn("Failed to save bridge config: " + e.getMessage());
        }
    }

    public static BridgeConfig getConfig() {
        return config;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    private static void logInfo(String message) {
        if (SkillLevelingMod.getInstance() != null) {
            SkillLevelingMod.getInstance().getLogger().info(message);
        }
    }

    private static void logWarn(String message) {
        if (SkillLevelingMod.getInstance() != null) {
            SkillLevelingMod.getInstance().getLogger().warn(message);
        }
    }
}
