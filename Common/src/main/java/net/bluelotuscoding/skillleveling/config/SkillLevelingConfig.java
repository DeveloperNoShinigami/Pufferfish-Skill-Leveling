package net.bluelotuscoding.skillleveling.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.bluelotuscoding.skillleveling.util.AddonLogger;

public final class SkillLevelingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static boolean requireUnlockForImbuing = false;
    public static boolean requireUnlockForCurioImbuing = false;
    public static boolean debugLogging = false;

    private static File configFile;

    private SkillLevelingConfig() {
    }

    public static synchronized void load(File configDir) {
        configFile = new File(configDir, "puffish_skill_leveling.json");
        ConfigData data = new ConfigData();

        if (configFile.isFile()) {
            try (Reader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
                ConfigData parsed = GSON.fromJson(reader, ConfigData.class);
                if (parsed != null) {
                    data = parsed;
                }
            } catch (Exception e) {
                AddonLogger.LOGGER.warn("Failed to read puffish_skill_leveling.json; using defaults. " + e.getMessage());
            }
        }

        apply(data);
        save();
    }

    public static synchronized void save() {
        if (configFile == null) {
            return;
        }
        ConfigData data = snapshot();
        try {
            Files.createDirectories(configFile.toPath().getParent());
            try (Writer writer = Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            AddonLogger.LOGGER.warn("Failed to save puffish_skill_leveling.json. " + e.getMessage());
        }
    }

    private static void apply(ConfigData data) {
        requireUnlockForImbuing = data.require_unlock_for_imbuing;
        requireUnlockForCurioImbuing = data.require_unlock_for_curio_imbuing;
        debugLogging = data.debug_logging;
    }

    private static ConfigData snapshot() {
        ConfigData data = new ConfigData();
        data.require_unlock_for_imbuing = requireUnlockForImbuing;
        data.require_unlock_for_curio_imbuing = requireUnlockForCurioImbuing;
        data.debug_logging = debugLogging;
        return data;
    }

    private static final class ConfigData {
        boolean require_unlock_for_imbuing = false;
        boolean require_unlock_for_curio_imbuing = false;
        boolean debug_logging = false;
    }
}
