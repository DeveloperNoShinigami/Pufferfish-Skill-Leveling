package net.bluelotuscoding.skillleveling.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CLIENT-SIDE SKILL LEVEL STORAGE
 * 
 * Stores skill levels synchronized from the server for display in the UI.
 */
public class ClientSkillLevelStorage {
    private static final Map<String, Integer> skillLevels = new ConcurrentHashMap<>();
    private static final Map<String, Integer> skillMaxLevels = new ConcurrentHashMap<>();
    private static final Map<String, Integer> skillPointsPerLevel = new ConcurrentHashMap<>();

    // Mapping from definition ID to the key (for mixin lookups)
    private static final Map<String, String> definitionToKey = new ConcurrentHashMap<>();

    private static String getKey(String categoryId, String skillId) {
        return categoryId + ":" + skillId;
    }

    public static void setLevel(String categoryId, String skillId, int level, int maxLevel, int pointsPerLevel) {
        String key = getKey(categoryId, skillId);
        if (level <= 0) {
            // Level 0 means skill is locked/reset - remove from cache
            skillLevels.remove(key);
            skillMaxLevels.remove(key);
            skillPointsPerLevel.remove(key);
        } else {
            skillLevels.put(key, level);
            skillMaxLevels.put(key, maxLevel);
            skillPointsPerLevel.put(key, pointsPerLevel);
        }
    }

    /**
     * Register a mapping from definition ID to skill key.
     * Called when syncing descriptions so we can look up levels by definition.
     */
    public static void registerDefinitionMapping(String definitionId, String categoryId, String skillId) {
        definitionToKey.put(definitionId, getKey(categoryId, skillId));
    }

    /**
     * Get level by definition ID (used by ClientSkillDefinitionConfigMixin).
     * Returns 0 if no mapping found.
     */
    public static int getLevelByDefinitionId(String definitionId) {
        String key = definitionToKey.get(definitionId);
        if (key == null) {
            return 0;
        }
        return skillLevels.getOrDefault(key, 0);
    }

    /**
     * Get max level by definition ID.
     */
    public static int getMaxLevelByDefinitionId(String definitionId) {
        String key = definitionToKey.get(definitionId);
        if (key == null) {
            return 1;
        }
        return skillMaxLevels.getOrDefault(key, 1);
    }

    /**
     * Get points per level by definition ID.
     */
    public static int getPointsPerLevelByDefinitionId(String definitionId) {
        String key = definitionToKey.get(definitionId);
        if (key == null) {
            return 0; // Means not a leveled skill in this context
        }
        return skillPointsPerLevel.getOrDefault(key, 0);
    }

    public static int getLevel(String categoryId, String skillId) {

        // Default to 0 (not unlocked), not 1
        return skillLevels.getOrDefault(getKey(categoryId, skillId), 0);
    }

    public static int getMaxLevel(String categoryId, String skillId) {
        return skillMaxLevels.getOrDefault(getKey(categoryId, skillId), 1);
    }

    public static boolean hasLevelInfo(String categoryId, String skillId) {
        return skillLevels.containsKey(getKey(categoryId, skillId));
    }

    /**
     * Clear level info for a specific skill (used when skill is reset/locked).
     */
    public static void clearSkill(String categoryId, String skillId) {
        String key = getKey(categoryId, skillId);
        skillLevels.remove(key);
        skillMaxLevels.remove(key);
    }

    /**
     * Clear all level info for a category (used when category is reset).
     */
    public static void clearCategory(String categoryId) {
        String prefix = categoryId + ":";
        skillLevels.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
        skillMaxLevels.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    }

    /**
     * Clear all level info (used on disconnect/logout).
     */
    public static void clearAll() {
        skillLevels.clear();
        skillMaxLevels.clear();
        skillPointsPerLevel.clear();
        definitionToKey.clear();
    }

}
