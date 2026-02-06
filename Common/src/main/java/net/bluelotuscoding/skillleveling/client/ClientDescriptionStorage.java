package net.bluelotuscoding.skillleveling.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side storage for per-level skill descriptions.
 * Stores level descriptions received from server for tooltip display.
 */
public class ClientDescriptionStorage {

    // Structure: definitionId -> level -> description text
    private static final Map<String, Map<Integer, String>> levelDescriptions = new HashMap<>();
    private static final Map<String, Map<Integer, String>> levelExtraDescriptions = new HashMap<>();
    private static final Map<String, Boolean> mergeDescriptionFlags = new HashMap<>();
    private static final Map<String, Integer> maxLevels = new HashMap<>();
    private static final Map<String, String> lootModes = new HashMap<>();
    private static final Map<String, java.util.List<net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward.SkillPrerequisite>> skillPrerequisites = new HashMap<>();

    /**
     * Store level descriptions for a skill definition.
     */
    public static void setDescriptions(String definitionId,
            Map<Integer, String> descriptions,
            Map<Integer, String> extraDescriptions,
            boolean mergeDescription,
            int maxLevel,
            String lootMode,
            java.util.List<net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward.SkillPrerequisite> prerequisites) {
        if (descriptions != null && !descriptions.isEmpty()) {
            levelDescriptions.put(definitionId, new HashMap<>(descriptions));
        }
        if (extraDescriptions != null && !extraDescriptions.isEmpty()) {
            levelExtraDescriptions.put(definitionId, new HashMap<>(extraDescriptions));
        }
        mergeDescriptionFlags.put(definitionId, mergeDescription);
        maxLevels.put(definitionId, maxLevel);
        lootModes.put(definitionId, lootMode != null ? lootMode : "");
        if (prerequisites != null && !prerequisites.isEmpty()) {
            skillPrerequisites.put(definitionId, new java.util.ArrayList<>(prerequisites));
        }
    }

    /**
     * Get description for a specific level.
     * In normal mode: returns only current level text
     * In merged mode: returns all levels up to current, stacked with newlines
     */
    public static String getDescription(String definitionId, int currentLevel) {
        Map<Integer, String> descriptions = levelDescriptions.get(definitionId);
        if (descriptions == null || descriptions.isEmpty()) {
            return null; // No custom descriptions, use original
        }

        boolean merge = mergeDescriptionFlags.getOrDefault(definitionId, false);

        if (merge) {
            // Merged mode: stack all levels up to current
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= currentLevel; i++) {
                String desc = descriptions.get(i);
                if (desc != null) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    if (i == currentLevel) {
                        sb.append("§a").append(desc); // Highlight current
                    } else {
                        sb.append("§7").append(desc);
                    }
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        } else {
            // Normal mode: only current level
            return descriptions.get(currentLevel);
        }
    }

    /**
     * Get extra description (preview of next level).
     * In normal mode: returns next level text (or final message at max)
     * In merged mode: returns all extra texts stacked
     */
    public static String getExtraDescription(String definitionId, int currentLevel) {
        Map<Integer, String> extraDescs = levelExtraDescriptions.get(definitionId);
        if (extraDescs == null || extraDescs.isEmpty()) {
            return null;
        }

        boolean merge = mergeDescriptionFlags.getOrDefault(definitionId, false);
        int maxLevel = maxLevels.getOrDefault(definitionId, 1);

        if (merge) {
            // Merged mode: stack all extra descriptions up to current
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= currentLevel; i++) {
                String desc = extraDescs.get(i);
                if (desc != null) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append("§7").append(desc);
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        } else {
            // Normal mode: show next level or final message at max
            if (currentLevel >= maxLevel) {
                // At max - return final extra description (stored at maxLevel key)
                return extraDescs.get(maxLevel);
            } else {
                // Preview next level
                return extraDescs.get(currentLevel + 1);
            }
        }
    }

    /**
     * Check if we have custom descriptions for a definition.
     */
    public static boolean hasDescriptions(String definitionId) {
        return levelDescriptions.containsKey(definitionId)
                && !levelDescriptions.get(definitionId).isEmpty();
    }

    /**
     * Get max level for a definition.
     */
    public static int getMaxLevel(String definitionId) {
        return maxLevels.getOrDefault(definitionId, 1);
    }

    public static String getLootMode(String definitionId) {
        return lootModes.getOrDefault(definitionId, "");
    }

    public static java.util.List<net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward.SkillPrerequisite> getPrerequisites(
            String definitionId) {
        return skillPrerequisites.get(definitionId);
    }

    /**
     * Check if a skill is imbue-only.
     */
    public static boolean isImbueOnly(String definitionId) {
        return "imbue_only".equals(getLootMode(definitionId));
    }

    /**
     * Check if a skill is tome-only.
     */
    public static boolean isTomeOnly(String definitionId) {
        return "tome_only".equals(getLootMode(definitionId));
    }

    /**
     * Get description for a specific level - NEVER merges, always single level.
     */
    public static String getDescriptionSingle(String definitionId, int level) {
        Map<Integer, String> descriptions = levelDescriptions.get(definitionId);
        if (descriptions == null) {
            return null;
        }
        return descriptions.get(level);
    }

    /**
     * Get extra description for next level - NEVER merges.
     * Uses current level as the index:
     * - Index 0 = what level 1 gives (shown at level 0)
     * - Index 1 = what level 2 gives (shown at level 1)
     * - Index N = what level N+1 gives (shown at level N)
     * - At max level, uses max level index for final message
     */
    public static String getExtraDescriptionSingle(String definitionId, int currentLevel) {
        Map<Integer, String> extraDescs = levelExtraDescriptions.get(definitionId);
        if (extraDescs == null || extraDescs.isEmpty()) {
            return null;
        }

        int maxLevel = maxLevels.getOrDefault(definitionId, 1);

        // Use current level as the key directly
        // At max level or above, use max level key for final message
        int lookupKey = Math.min(currentLevel, maxLevel);
        if (lookupKey < 0) {
            lookupKey = 0;
        }

        return extraDescs.get(lookupKey);
    }

    /**
     * Clear a specific definition's descriptions.
     */
    public static void clearDefinition(String definitionId) {
        levelDescriptions.remove(definitionId);
        levelExtraDescriptions.remove(definitionId);
        mergeDescriptionFlags.remove(definitionId);
        maxLevels.remove(definitionId);
        lootModes.remove(definitionId);
        skillPrerequisites.remove(definitionId);
    }

    /**
     * Clear all stored descriptions (e.g., on disconnect).
     */
    public static void clearAll() {
        levelDescriptions.clear();
        levelExtraDescriptions.clear();
        mergeDescriptionFlags.clear();
        maxLevels.clear();
        lootModes.clear();
        skillPrerequisites.clear();
    }

    public static java.util.Set<String> getAllKeys() {
        return levelDescriptions.keySet();
    }
}
