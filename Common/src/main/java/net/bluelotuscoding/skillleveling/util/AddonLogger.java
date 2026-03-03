package net.bluelotuscoding.skillleveling.util;

import net.puffish.skillsmod.util.PrefixedLogger;

/**
 * ADDON LOGGING SYSTEM: Provides consistent logging for the skill leveling
 * addon
 * 
 * This wraps the Skills mod's PrefixedLogger to ensure our addon messages are
 * properly formatted and distinguishable from core Skills mod messages, while
 * maintaining consistency with the overall logging style.
 * 
 * LOG LEVELS:
 * - INFO: Important addon events (initialization, data synchronization)
 * - WARN: Potential issues (data inconsistencies, configuration problems)
 * - ERROR: Critical problems (data corruption, integration failures)
 * - DEBUG: Detailed operational information (level changes, reward triggers)
 */
public class AddonLogger {

    public static final AddonLogger LOGGER = new AddonLogger();
    private final PrefixedLogger logger;

    public AddonLogger() {
        // ADDON PREFIX: Clearly identifies our addon in the logs
        this.logger = new PrefixedLogger("puffish_skill_leveling");
    }

    /**
     * INFORMATION LOGGING: For important addon events and status updates
     */
    public void info(String message) {
        logger.info("[ADDON] " + message);
    }

    /**
     * WARNING LOGGING: For potential issues that don't break functionality
     */
    public void warn(String message) {
        logger.warn("[ADDON] " + message);
    }

    /**
     * ERROR LOGGING: For serious problems that may affect addon functionality
     */
    public void error(String message) {
        logger.error("[ADDON] " + message);
    }

    /**
     * DEBUG LOGGING: For detailed operational information during development.
     * SILENCED by default to prevent log clogging.
     */
    public void debug(String message) {
        if (net.bluelotuscoding.skillleveling.config.SkillLevelingConfig.debugLogging) {
            logger.info("[DEBUG] " + message);
        }
    }
}
