package net.bluelotuscoding.skillleveling.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles persistent configuration for the Skill Leveling addon.
 */
public class SkillLevelingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    // Config options
    public static boolean disableSkillMasterHouse = false;
    public static boolean requireUnlockForImbuing = false; // Whether regular gear bonuses require the base skill to be
                                                           // unlocked
    public static boolean requireUnlockForCurioImbuing = false; // Whether Curio bonuses require the base skill to be
                                                                // unlocked
    public static boolean debugLogging = false; // Whether to enable massive debug logging

    public static void load(File configDir) {
        configFile = new File(configDir, "puffish_skill_leveling.json");

        if (!configFile.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json.has("disable_skill_master_house")) {
                disableSkillMasterHouse = json.get("disable_skill_master_house").getAsBoolean();
            }
            if (json.has("require_unlock_for_imbuing")) {
                requireUnlockForImbuing = json.get("require_unlock_for_imbuing").getAsBoolean();
            }
            if (json.has("require_unlock_for_curio_imbuing")) {
                requireUnlockForCurioImbuing = json.get("require_unlock_for_curio_imbuing").getAsBoolean();
            }
            if (json.has("debug_logging")) {
                debugLogging = json.get("debug_logging").getAsBoolean();
            }
        } catch (IOException e) {
            SkillLevelingMod.getInstance().getLogger().error("Failed to load config: " + e.getMessage());
        }
    }

    public static void save() {
        if (configFile == null)
            return;

        try {
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
        } catch (Exception e) {
            // Log error but try to continue
        }

        JsonObject json = new JsonObject();
        json.addProperty("disable_skill_master_house", disableSkillMasterHouse);
        json.addProperty("require_unlock_for_imbuing", requireUnlockForImbuing);
        json.addProperty("require_unlock_for_curio_imbuing", requireUnlockForCurioImbuing);
        json.addProperty("debug_logging", debugLogging);

        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            SkillLevelingMod.getInstance().getLogger().error("Failed to save config: " + e.getMessage());
        }
    }
}
