package net.bluelotuscoding.skillleveling.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores leveled configuration data parsed from skill definitions.
 * This is populated by SkillDefinitionConfigMixin and accessed by
 * SkillLevelingManager.
 */
public class LeveledConfigStorage {

    private static final Map<String, LeveledConfig> leveledConfigs = new HashMap<>();

    public static void put(String skillId, LeveledConfig leveledConfig) {
        leveledConfigs.put(skillId, leveledConfig);
    }

    public static LeveledConfig get(String skillId) {
        return leveledConfigs.get(skillId);
    }

    public static Map<String, LeveledConfig> getAllEntries() {
        return new HashMap<>(leveledConfigs);
    }

    public static boolean has(String skillId) {
        return leveledConfigs.containsKey(skillId);
    }

    public static void clear() {
        leveledConfigs.clear();
    }

    public static class LeveledConfig {
        public final int maxLevels;
        public final int pointsPerLevel;
        public final boolean mergeDescription;
        public final List<RequiredSkillEntry> requiredSkills;
        public final String lootMode;
        public final String categoryId;

        public LeveledConfig(int maxLevels, int pointsPerLevel, boolean mergeDescription, String lootMode,
                String categoryId) {
            this(maxLevels, pointsPerLevel, mergeDescription, new ArrayList<>(), lootMode, categoryId);
        }

        public LeveledConfig(int maxLevels, int pointsPerLevel, boolean mergeDescription,
                List<RequiredSkillEntry> requiredSkills, String lootMode, String categoryId) {
            this.maxLevels = maxLevels;
            this.pointsPerLevel = pointsPerLevel;
            this.mergeDescription = mergeDescription;
            this.requiredSkills = requiredSkills != null ? requiredSkills : new ArrayList<>();
            this.lootMode = lootMode;
            this.categoryId = categoryId;
        }
    }

    public static class RequiredSkillEntry {
        public final String skillId;
        public final int minLevel;

        public RequiredSkillEntry(String skillId, int minLevel) {
            this.skillId = skillId;
            this.minLevel = minLevel;
        }
    }
}
