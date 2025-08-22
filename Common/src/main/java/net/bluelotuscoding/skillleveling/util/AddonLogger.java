package net.bluelotuscoding.skillleveling.util;

import net.puffish.skillsmod.util.PrefixedLogger;

/**
 * ADDON LOGGING SYSTEM: Provides consistent logging for the skill leveling addon
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
     * DEBUG LOGGING: For detailed operational information during development
     */
    public void debug(String message) {
        // Note: Skills mod's PrefixedLogger doesn't have debug level,
        // so we use info with DEBUG prefix for development builds
        logger.info("[ADDON] [DEBUG] " + message);
    }
}
