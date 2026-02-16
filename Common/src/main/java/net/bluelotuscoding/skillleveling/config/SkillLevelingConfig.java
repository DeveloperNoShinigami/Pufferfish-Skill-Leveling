package net.bluelotuscoding.skillleveling.config;

import java.io.File;

/**
 * Configuration placeholder for the Skill Leveling addon.
 *
 * All config options are currently disabled and planned for future implementation.
 * The debug logging toggle is available as a runtime-only command (/skillleveling debug)
 * but does not persist across restarts.
 *
 * Planned future config options:
 * - disable_skill_master_house: Prevent Skill Master House structures from generating
 * - require_unlock_for_imbuing: Imbued gear bonuses require the base skill to be unlocked
 * - require_unlock_for_curio_imbuing: Curio (Skill Charm) bonuses require the base skill to be unlocked
 * - debug_logging: Enable verbose debug logging (persistent)
 */
public class SkillLevelingConfig {

    // Runtime-only toggle (not persisted to file) — toggled via /skillleveling debug command
    public static boolean debugLogging = false;

    /**
     * Placeholder for future config file loading.
     * Currently a no-op — all options are hardcoded to their defaults.
     */
    public static void load(File configDir) {
        // Config file loading is planned for a future update.
        // All options are currently hardcoded to their default values.
    }

    /**
     * Placeholder for future config file saving.
     * Currently a no-op.
     */
    public static void save() {
        // Config file saving is planned for a future update.
    }
}
