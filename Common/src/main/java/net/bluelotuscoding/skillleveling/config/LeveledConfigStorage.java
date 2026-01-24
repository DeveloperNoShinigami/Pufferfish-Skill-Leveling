package net.bluelotuscoding.skillleveling.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores leveled configuration data parsed from skill definitions.
 * This is populated by SkillDefinitionConfigMixin and accessed by
 * SkillLevelingManager.
 * 
 * Uses skill ID as key since object references may differ between
 * parsing phase and loading phase.
 */
public class LeveledConfigStorage {

    private static final Map<String, LeveledConfig> leveledConfigs = new HashMap<>();

    public static void put(String skillId, LeveledConfig leveledConfig) {
        leveledConfigs.put(skillId, leveledConfig);
    }

    public static LeveledConfig get(String skillId) {
        return leveledConfigs.get(skillId);
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

        public LeveledConfig(int maxLevels, int pointsPerLevel, boolean mergeDescription) {
            this(maxLevels, pointsPerLevel, mergeDescription, new ArrayList<>());
        }

        public LeveledConfig(int maxLevels, int pointsPerLevel, boolean mergeDescription,
                List<RequiredSkillEntry> requiredSkills) {
            this.maxLevels = maxLevels;
            this.pointsPerLevel = pointsPerLevel;
            this.mergeDescription = mergeDescription;
            this.requiredSkills = requiredSkills != null ? requiredSkills : new ArrayList<>();
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
