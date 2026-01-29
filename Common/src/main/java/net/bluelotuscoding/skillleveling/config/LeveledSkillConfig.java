package net.bluelotuscoding.skillleveling.config;

import net.puffish.skillsmod.api.json.JsonObject;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Configuration for a skill that supports multiple levels
 */
public class LeveledSkillConfig {

    private final String skillId;
    private final int maxLevel;
    private final int pointsPerLevel;
    private final Map<Integer, List<LevelRewardConfig>> levelRewards;
    private final Map<Integer, String> levelDescriptions;
    private final Map<Integer, List<String>> levelRequirements;
    private final List<RequiredSkill> requiredSkills;

    public LeveledSkillConfig(
            String skillId,
            int maxLevel,
            int pointsPerLevel,
            Map<Integer, List<LevelRewardConfig>> levelRewards,
            Map<Integer, String> levelDescriptions,
            Map<Integer, List<String>> levelRequirements,
            List<RequiredSkill> requiredSkills) {
        this.skillId = skillId;
        this.maxLevel = maxLevel;
        this.pointsPerLevel = pointsPerLevel;
        this.levelRewards = levelRewards;
        this.levelDescriptions = levelDescriptions;
        this.levelRequirements = levelRequirements;
        this.requiredSkills = requiredSkills != null ? requiredSkills : new ArrayList<>();
    }

    public String getSkillId() {
        return skillId;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getPointsPerLevel() {
        return pointsPerLevel;
    }

    public Map<Integer, List<LevelRewardConfig>> getLevelRewards() {
        return levelRewards;
    }

    public Map<Integer, String> getLevelDescriptions() {
        return levelDescriptions;
    }

    public Map<Integer, List<String>> getLevelRequirements() {
        return levelRequirements;
    }

    public List<RequiredSkill> getRequiredSkills() {
        return requiredSkills;
    }

    /**
     * Get the description for a specific level
     */
    public String getDescriptionForLevel(int level) {
        return levelDescriptions.getOrDefault(level, "");
    }

    /**
     * Get the requirements for a specific level
     */
    public List<String> getRequirementsForLevel(int level) {
        return levelRequirements.getOrDefault(level, new ArrayList<>());
    }

    /**
     * Get the rewards for a specific level
     */
    public List<LevelRewardConfig> getRewardsForLevel(int level) {
        return levelRewards.getOrDefault(level, new ArrayList<>());
    }

    /**
     * Configuration for a reward at a specific level
     */
    public static class LevelRewardConfig {
        private final String type;
        private final JsonObject data;

        public LevelRewardConfig(String type, JsonObject data) {
            this.type = type;
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public JsonObject getData() {
            return data;
        }
    }

    /**
     * Represents a required skill with a minimum level
     */
    public static class RequiredSkill {
        private final String skillId;
        private final int minLevel;

        public RequiredSkill(String skillId, int minLevel) {
            this.skillId = skillId;
            this.minLevel = minLevel;
        }

        public String getSkillId() {
            return skillId;
        }

        public int getMinLevel() {
            return minLevel;
        }
    }

    /**
     * Builder for creating LeveledSkillConfig instances
     */
    public static class Builder {
        private String skillId;
        private int maxLevel = 1;
        private int pointsPerLevel = 0;
        private final Map<Integer, List<LevelRewardConfig>> levelRewards = new HashMap<>();
        private final Map<Integer, String> levelDescriptions = new HashMap<>();
        private final Map<Integer, List<String>> levelRequirements = new HashMap<>();
        private final List<RequiredSkill> requiredSkills = new ArrayList<>();

        public Builder skillId(String skillId) {
            this.skillId = skillId;
            return this;
        }

        public Builder maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        public Builder pointsPerLevel(int pointsPerLevel) {
            this.pointsPerLevel = pointsPerLevel;
            return this;
        }

        public Builder addLevelReward(int level, String type, JsonObject data) {
            levelRewards.computeIfAbsent(level, k -> new ArrayList<>())
                    .add(new LevelRewardConfig(type, data));
            return this;
        }

        public Builder addLevelDescription(int level, String description) {
            levelDescriptions.put(level, description);
            return this;
        }

        public Builder addLevelRequirement(int level, String requirement) {
            levelRequirements.computeIfAbsent(level, k -> new ArrayList<>())
                    .add(requirement);
            return this;
        }

        public Builder addRequiredSkill(String skillId, int minLevel) {
            requiredSkills.add(new RequiredSkill(skillId, minLevel));
            return this;
        }

        public LeveledSkillConfig build() {
            if (skillId == null) {
                throw new IllegalStateException("skillId must be set");
            }
            return new LeveledSkillConfig(skillId, maxLevel, pointsPerLevel,
                    levelRewards, levelDescriptions, levelRequirements, requiredSkills);
        }
    }
}
