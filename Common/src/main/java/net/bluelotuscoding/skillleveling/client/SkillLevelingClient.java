package net.bluelotuscoding.skillleveling.client;

import net.minecraft.text.Text;

/**
 * CLIENT-SIDE SKILL LEVEL DISPLAY INTEGRATION
 * 
 * PURPOSE: Provides client-side utilities for displaying skill level
 * information
 * in chat messages, action bars, and other UI elements. Works with server-side
 * synchronization to show real-time skill progression feedback.
 * 
 * ARCHITECTURE: Lightweight client helper that formats skill information for
 * display without requiring complex networking. Relies on server action bar
 * messages and chat notifications for real-time updates.
 */
public class SkillLevelingClient {

    // ================================================
    // CLIENT DISPLAY UTILITIES
    // ================================================

    /**
     * PROGRESS BAR GENERATOR: Creates visual progress indicators
     * 
     * VISUAL MECHANICS: Generates colored progress bars for skill advancement
     * that can be displayed in chat or action bar messages.
     */
    public static String createProgressBar(int current, int max, int barLength) {
        if (max <= 0) {
            return "§c[ERROR]";
        }

        if (current >= max) {
            // COMPLETED BAR: Full green progress bar for maxed skills
            StringBuilder bar = new StringBuilder("§a");
            for (int i = 0; i < barLength; i++) {
                bar.append("█");
            }
            bar.append(" §a100%");
            return bar.toString();
        }

        // PROGRESS CALCULATION: Calculate filled vs empty portions
        float progress = (float) current / max;
        int filled = Math.round(progress * barLength);

        StringBuilder bar = new StringBuilder();

        // FILLED PORTION: Green bars for completed progress
        bar.append("§a");
        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }

        // EMPTY PORTION: Gray bars for remaining progress
        bar.append("§7");
        for (int i = filled; i < barLength; i++) {
            bar.append("█");
        }

        // PERCENTAGE: Add numeric progress indicator
        bar.append(String.format(" §7%.0f%%", progress * 100));

        return bar.toString();
    }

    /**
     * LEVEL DISPLAY FORMATTER: Creates formatted level display text
     * 
     * FORMATTING MECHANICS: Standardizes how skill levels are displayed
     * across different UI contexts (chat, tooltips, commands, etc.).
     */
    public static String formatSkillLevel(String skillName, int current, int max) {
        if (current >= max) {
            return String.format("§6%s §7- Level §6%d §a(MAX)", skillName, current);
        } else {
            return String.format("§6%s §7- Level §6%d§7/§6%d", skillName, current, max);
        }
    }

    /**
     * SKILL ADVANCEMENT NOTIFICATION: Creates level-up message
     * 
     * NOTIFICATION MECHANICS: Generates celebratory messages for skill
     * advancement that can be sent as chat or action bar notifications.
     */
    public static String createLevelUpMessage(String skillName, int newLevel, int maxLevel) {
        if (newLevel >= maxLevel) {
            return String.format("§6✦ %s §aMAXED at level §6%d§a! ✦", skillName, newLevel);
        } else {
            return String.format("§6⬆ %s §aadvanced to level §6%d§7/§6%d§a!", skillName, newLevel, maxLevel);
        }
    }

    /**
     * SKILL REFUND NOTIFICATION: Creates refund confirmation message
     * 
     * REFUND MECHANICS: Generates messages confirming skill level refunds
     * with clear indication of new level and point recovery.
     */
    public static String createRefundMessage(String skillName, int newLevel, int pointsRefunded) {
        if (pointsRefunded > 0) {
            return String.format("§e⬇ %s §7refunded to level §6%d §7(+§a%d §7points)",
                    skillName, newLevel, pointsRefunded);
        } else {
            return String.format("§e⬇ %s §7refunded to level §6%d", skillName, newLevel);
        }
    }

    /**
     * POINT COST DISPLAY: Shows advancement cost information
     * 
     * COST MECHANICS: Formats point cost information for display in
     * commands and UI elements to help players plan advancement.
     */
    public static String formatPointCost(int cost, int available) {
        if (cost <= 0) {
            return "§aFree";
        }

        if (available >= cost) {
            return String.format("§a%d points", cost);
        } else {
            int needed = cost - available;
            return String.format("§c%d points §7(need §c%d §7more)", cost, needed);
        }
    }

    // ================================================
    // TOOLTIP ENHANCEMENT HELPERS
    // ================================================

    /**
     * SKILL TOOLTIP ENHANCER: Adds level information to skill tooltips
     * 
     * TOOLTIP MECHANICS: Provides formatted text that can be added to
     * existing skill tooltips to show progression information.
     */
    public static java.util.List<String> enhanceSkillTooltip(String skillName, int currentLevel, int maxLevel,
            String description) {
        java.util.List<String> tooltip = new java.util.ArrayList<>();

        // SKILL HEADER: Add formatted skill name and level
        tooltip.add(formatSkillLevel(skillName, currentLevel, maxLevel));

        // PROGRESS BAR: Add visual progress indicator
        tooltip.add(createProgressBar(currentLevel, maxLevel, 10));

        // DESCRIPTION: Add skill description if available
        if (description != null && !description.isEmpty()) {
            tooltip.add("");
            tooltip.add("§7" + description);
        }

        // ADVANCEMENT HINT: Add progression information
        if (currentLevel < maxLevel) {
            tooltip.add("");
            tooltip.add("§8Use commands to advance this skill");
        }

        return tooltip;
    }

    /**
     * COMMAND FEEDBACK FORMATTER: Creates standardized command response messages
     * 
     * FEEDBACK MECHANICS: Ensures consistent formatting across all skill
     * leveling commands for professional user experience.
     */
    public static String formatCommandSuccess(String action, String skillName, int level) {
        switch (action.toLowerCase()) {
            case "advance":
                return String.format("§a✓ Successfully advanced §e%s §ato level §6%d", skillName, level);
            case "set":
                return String.format("§a✓ Successfully set §e%s §ato level §6%d", skillName, level);
            case "refund":
                return String.format("§a✓ Successfully refunded §e%s §ato level §6%d", skillName, level);
            default:
                return String.format("§a✓ Successfully modified §e%s §a(level §6%d§a)", skillName, level);
        }
    }

    /**
     * ERROR MESSAGE FORMATTER: Creates standardized error messages
     * 
     * ERROR MECHANICS: Provides consistent error formatting to help users
     * understand what went wrong with their skill operations.
     */
    public static String formatCommandError(String action, String skillName, String reason) {
        return String.format("§c✗ Failed to %s §e%s§c: %s", action, skillName, reason);
    }

    // ================================================
    // REAL-TIME UPDATE PROCESSORS
    // ================================================

    /**
     * ACTION BAR PROCESSOR: Handles action bar skill updates
     * 
     * UPDATE MECHANICS: Processes server-sent action bar messages to
     * provide immediate visual feedback for skill changes.
     */
    public static void processSkillUpdate(String actionBarMessage) {
        // ACTION BAR PARSING: Extract skill information from server messages
        if (actionBarMessage.contains("⬆") && actionBarMessage.contains("level")) {
            // LEVEL UP PROCESSING: Enhanced display for advancement
            // In a real implementation, this could trigger client-side effects
            // like particles, sounds, or UI animations
            // Currently handles display through action bar messaging
            handleLevelUpEffect(actionBarMessage);
        } else if (actionBarMessage.contains("⬇") && actionBarMessage.contains("refunded")) {
            // REFUND PROCESSING: Handle refund notifications
            // Could trigger different visual effects for refunds
            // Currently handles display through action bar messaging
            handleRefundEffect(actionBarMessage);
        }
    }

    private static void handleLevelUpEffect(String message) {
        // Future: Client-side level up effects
    }

    private static void handleRefundEffect(String message) {
        // Future: Client-side refund effects
    }

    /**
     * CHAT MESSAGE ENHANCER: Improves chat-based skill notifications
     * 
     * ENHANCEMENT MECHANICS: Adds visual elements and formatting to
     * chat messages about skill progression for better visibility.
     */
    public static Text enhanceChatMessage(String message) {
        // MESSAGE ENHANCEMENT: Add visual elements to chat notifications
        if (message.contains("⬆") || message.contains("level")) {
            // ADVANCEMENT ENHANCEMENT: Add visual flair to level-up messages
            return Text.literal("§6━━━ " + message + " §6━━━");
        } else if (message.contains("⬇") || message.contains("refund")) {
            // REFUND ENHANCEMENT: Distinct styling for refund messages
            return Text.literal("§e╰─ " + message + " ─╯");
        }

        return Text.literal(message);
    }
}
