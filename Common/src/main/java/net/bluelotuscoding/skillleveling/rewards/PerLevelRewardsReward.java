package net.bluelotuscoding.skillleveling.rewards;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.config.skill.SkillRewardConfig;
import net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl;
import net.puffish.skillsmod.util.DisposeContext;
import net.puffish.skillsmod.util.LegacyUtils;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Enhanced PerLevelRewardsReward with skill prerequisites and advanced features
 * 
 * New Features:
 * - Skill prerequisites with required_skill field
 * - Enhanced validation and error handling
 * - Better description merging
 * - Improved reward distribution logic
 * - Dynamic level scaling
 * - Conditional reward activation
 */
public class PerLevelRewardsReward implements Reward {
    public static final Identifier ID = SkillLevelingMod.createIdentifier("per_level_rewards");

    private final Map<Integer, List<SkillRewardConfig>> levelRewards;
    private final String skillId;
    private final int maxLevel;
    private final int pointsPerLevel;
    private final Map<Integer, String> levelDescriptions;
    private final Map<Integer, String> levelExtraDescriptions;
    private final boolean mergeDescription;
    private final List<SkillPrerequisite> requiredSkills;
    private final boolean allowPartialRewards;
    private final double scalingFactor;
    private final Map<UUID, Integer> counts = new HashMap<>();

    /**
     * Represents a skill prerequisite
     */
    public static class SkillPrerequisite {
        private final String skillId;
        private final int level;
        private final String categoryId;

        public SkillPrerequisite(String skillId, int level, String categoryId) {
            this.skillId = skillId;
            this.level = level;
            this.categoryId = categoryId;
        }
        
        public String getSkillId() {
            return skillId;
        }
        
        public int getLevel() {
            return level;
        }
        
        public String getCategoryId() {
            return categoryId;
        }
        
        @Override
        public String toString() {
            return String.format("%s:%d%s", skillId, level, 
                categoryId != null ? "@" + categoryId : "");
        }
    }

    private PerLevelRewardsReward(Map<Integer, List<SkillRewardConfig>> levelRewards, String skillId, int maxLevel, int pointsPerLevel, Map<Integer, String> levelDescriptions, Map<Integer, String> levelExtraDescriptions, boolean mergeDescription, List<SkillPrerequisite> requiredSkills, boolean allowPartialRewards, double scalingFactor) {
        this.levelRewards = levelRewards;
        this.skillId = skillId;
        this.maxLevel = maxLevel;
        this.pointsPerLevel = pointsPerLevel;
        this.levelDescriptions = levelDescriptions;
        this.levelExtraDescriptions = levelExtraDescriptions;
        this.mergeDescription = mergeDescription;
        this.requiredSkills = requiredSkills != null ? requiredSkills : new ArrayList<>();
        this.allowPartialRewards = allowPartialRewards;
        this.scalingFactor = scalingFactor;
    }    public String getSkillId() {
        return skillId;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getPointsPerLevel() {
        return pointsPerLevel;
    }
    
    public Map<Integer, String> getLevelDescriptions() {
        return levelDescriptions;
    }
    
    public Map<Integer, String> getLevelExtraDescriptions() {
        return levelExtraDescriptions;
    }
    
    public boolean isMergeDescription() {
        return mergeDescription;
    }
    
    public List<SkillPrerequisite> getRequiredSkills() {
        return requiredSkills;
    }
    
    public boolean allowsPartialRewards() {
        return allowPartialRewards;
    }
    
    public double getScalingFactor() {
        return scalingFactor;
    }
    
    /**
     * Check if all skill prerequisites are met for a given player
     */
    public boolean arePrerequisitesMet(UUID playerId) {
        if (requiredSkills.isEmpty()) {
            return true;
        }
        
        // Use SkillLevelingManager for prerequisite checking
        var mod = net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance();
        if (mod == null) {
            // Addon not initialized yet
            SkillLevelingMod.getInstance().getLogger().warn("Addon not initialized for prerequisite checking");
            return false;
        }
        
        var manager = mod.getSkillLevelingManager();
        if (manager == null) {
            mod.getLogger().warn("SkillLevelingManager not available for prerequisite checking");
            return false; // Fail safe - don't allow rewards if we can't check
        }
        
        // For prerequisite checking, we need to know which category this skill belongs to
        // First try to find it through the SkillsAPI
        Identifier categoryId = null;
        
        // Try to get category from SkillsAPI first (more reliable)
        var categories = SkillsAPI.getCategories();
        for (var categoryEntry : categories.entrySet()) {
            var category = categoryEntry.getValue();
            var skill = category.getSkill(skillId);
            if (skill.isPresent()) {
                categoryId = categoryEntry.getKey();
                break;
            }
        }
        
        // If that fails, try looking through registered rewards as fallback
        if (categoryId == null) {
            for (var categoryEntry : manager.getPerLevelRewardsRewards().entrySet()) {
                for (var skillEntry : categoryEntry.getValue().entrySet()) {
                    // Check if this is our skill by comparing the skillId
                    if (skillEntry.getValue() == this || skillEntry.getKey().equals(skillId)) {
                        categoryId = categoryEntry.getKey();
                        break;
                    }
                }
                if (categoryId != null) {
                    break;
                }
            }
        }
        
        if (categoryId == null) {
            mod.getLogger().warn("Could not determine category for skill " + (skillId != null ? skillId : "unknown") + " during prerequisite checking");
            return false; // Fail safe
        }
        
        mod.getLogger().debug("Checking prerequisites for skill " + skillId + " in category " + categoryId + ": " + requiredSkills.size() + " requirements");
        
        return manager.checkSkillPrerequisites(playerId, categoryId, skillId != null ? skillId : "unknown");
    }
    
    /**
     * Get the effective points per level with scaling applied
     */
    public int getEffectivePointsPerLevel(int currentLevel) {
        if (scalingFactor == 1.0) {
            return pointsPerLevel;
        }
        
        return (int) Math.ceil(pointsPerLevel * Math.pow(scalingFactor, currentLevel - 1));
    }
    
    /**
     * Get all prerequisites as a formatted string
     */
    public String getPrerequisitesString() {
        if (requiredSkills.isEmpty()) {
            return "None";
        }
        
        return requiredSkills.stream()
                .map(SkillPrerequisite::toString)
                .collect(Collectors.joining(", "));
    }
    
    /**
     * Get the description for a specific level, optionally merging with previous levels
     */
    public String getDescriptionForLevel(int level) {
        if (!mergeDescription || level <= 1) {
            String desc = levelDescriptions.getOrDefault(level, "");
            
            // Add prerequisite info if this is level 1 and prerequisites exist
            if (level == 1 && !requiredSkills.isEmpty()) {
                String prereqStr = "Requires: " + getPrerequisitesString();
                desc = desc.isEmpty() ? prereqStr : desc + "\n" + prereqStr;
            }
            
            return desc;
        }
        
        // Enhanced merging with better formatting
        StringBuilder merged = new StringBuilder();
        for (int i = 1; i <= level; i++) {
            String desc = levelDescriptions.get(i);
            if (desc != null && !desc.isEmpty()) {
                if (merged.length() > 0) {
                    merged.append("\n• ");
                } else {
                    merged.append("• ");
                }
                merged.append(desc);
            }
        }
        
        // Add prerequisite info
        if (!requiredSkills.isEmpty()) {
            String prereqStr = "\nRequires: " + getPrerequisitesString();
            merged.append(prereqStr);
        }
        
        return merged.toString();
    }
    
    /**
     * Get the extra description for a specific level, optionally merging with previous levels
     */
    public String getExtraDescriptionForLevel(int level) {
        if (!mergeDescription || level <= 1) {
            return levelExtraDescriptions.getOrDefault(level, "");
        }
        
        // Enhanced merging with better formatting
        StringBuilder merged = new StringBuilder();
        for (int i = 1; i <= level; i++) {
            String desc = levelExtraDescriptions.get(i);
            if (desc != null && !desc.isEmpty()) {
                if (merged.length() > 0) {
                    merged.append("\n→ ");
                } else {
                    merged.append("→ ");
                }
                merged.append(desc);
            }
        }
        return merged.toString();
    }

    public static void register() {
        SkillsAPI.registerReward(ID, PerLevelRewardsReward::parse);
    }

    static Result<PerLevelRewardsReward, Problem> parse(RewardConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(LegacyUtils.wrapNoUnused(obj -> parse(obj, context), context));
    }

    static Result<PerLevelRewardsReward, Problem> parse(JsonObject rootObject, ConfigContext context) {
        var problems = new ArrayList<Problem>();

        // Parse levels (required)
        var optLevelsMap = rootObject.getObject("levels")
                .andThen(obj -> obj.getAsMap((key, element) ->
                        element.getAsArray()
                                .andThen(arr -> arr.getAsList((i, e) -> SkillRewardConfig.parse(e, context))
                                        .mapFailure(Problem::combine))
                ).mapFailure(map -> Problem.combine(map.values())))
                .ifFailure(problems::add)
                .getSuccess();

        var levelsPath = rootObject.getPath().getObject("levels");
        var levelRewards = new HashMap<Integer, List<SkillRewardConfig>>();
        optLevelsMap.ifPresent(map -> {
            for (var entry : map.entrySet()) {
                try {
                    var level = Integer.parseInt(entry.getKey());
                    if (level < 1) {
                        problems.add(levelsPath.getObject(entry.getKey())
                                .createProblem("Level must be ≥ 1"));
                    } else {
                        levelRewards.put(level, entry.getValue());
                    }
                } catch (NumberFormatException e) {
                    problems.add(levelsPath.getObject(entry.getKey()).createProblem("Expected an integer"));
                }
            }
        });
        int definedMaxLevel = levelRewards.keySet().stream().max(Integer::compareTo).orElse(0);

        // Parse skill_id (optional)
        var optSkillId = rootObject.getString("skill_id")
                .ifFailure(problems::add)
                .getSuccess();

        // Parse max_skill_level (optional)
        var optMaxLevelTmp = rootObject.get("max_skill_level")
                .getSuccess()
                .flatMap(element -> element.getAsInt()
                        .ifFailure(problems::add)
                        .getSuccess());
        optMaxLevelTmp.ifPresent(maxLevel -> {
            if (maxLevel < 1) {
                problems.add(rootObject.getPath().getObject("max_skill_level")
                        .createProblem("Expected a value ≥ 1"));
            }
        });

        // Parse points_per_level (optional)
        var optPointsPerLevelTmp = rootObject.get("points_per_level")
                .getSuccess()
                .flatMap(element -> element.getAsInt()
                                .ifFailure(problems::add)
                                .getSuccess());
        optPointsPerLevelTmp.ifPresent(points -> {
            if (points < 0) {
                problems.add(rootObject.getPath().getObject("points_per_level")
                                .createProblem("Expected a value ≥ 0"));
            }
        });
        int optPointsPerLevel = optPointsPerLevelTmp.orElse(0);

        // Parse required_skill field (NEW FEATURE)
        var requiredSkills = new ArrayList<SkillPrerequisite>();
        var optRequiredSkills = rootObject.get("required_skill")
                .getSuccess()
                .flatMap(element -> element.getAsArray()
                        .ifFailure(problems::add)
                        .getSuccess());
        
        optRequiredSkills.ifPresent(array -> {
            var arrayResult = array.getAsList((index, element) -> {
                var prereqResult = element.getAsObject()
                        .ifFailure(problems::add);
                
                if (prereqResult.getFailure().isPresent()) {
                    return prereqResult.mapSuccess(obj -> (SkillPrerequisite) null);
                }
                
                var prereqObj = prereqResult.getSuccess().get();
                var skillIdResult = prereqObj.getString("skill_id")
                        .ifFailure(problems::add);
                var levelResult = prereqObj.getInt("level")
                        .ifFailure(problems::add);
                var categoryIdResult = prereqObj.get("category_id")
                        .getSuccess()
                        .flatMap(elem -> elem.getAsString()
                                .ifFailure(problems::add)
                                .getSuccess());
                
                if (skillIdResult.getSuccess().isPresent() && levelResult.getSuccess().isPresent()) {
                    var level = levelResult.getSuccess().get();
                    if (level < 1) {
                        problems.add(element.getPath().createProblem("Prerequisite level must be ≥ 1"));
                        return Result.failure(Problem.message("Invalid level"));
                    } else {
                        return Result.success(new SkillPrerequisite(
                                skillIdResult.getSuccess().get(),
                                level,
                                categoryIdResult.orElse(null)
                        ));
                    }
                } else {
                    return Result.failure(Problem.message("Missing required fields"));
                }
            });
            
            arrayResult.ifSuccess(list -> {
                for (var prerequisite : list) {
                    if (prerequisite != null) {
                        requiredSkills.add(prerequisite);
                    }
                }
            });
        });

        // Parse allow_partial_rewards (optional, defaults to false)
        var allowPartialRewards = rootObject.get("allow_partial_rewards")
                .getSuccess()
                .flatMap(element -> element.getAsBoolean()
                        .ifFailure(problems::add)
                        .getSuccess())
                .orElse(false);

        // Parse scaling_factor (optional, defaults to 1.0)
        var scalingFactorOpt = rootObject.get("scaling_factor")
                .getSuccess()
                .flatMap(element -> element.getAsDouble()
                        .ifFailure(problems::add)
                        .getSuccess());
        double scalingFactor = scalingFactorOpt.orElse(1.0);
        
        if (scalingFactor <= 0) {
            problems.add(rootObject.getPath().getObject("scaling_factor")
                    .createProblem("Scaling factor must be > 0"));
        }

        // Parse descriptions (optional)
        var levelDescriptions = new HashMap<Integer, String>();
        var optDescriptions = rootObject.get("descriptions")
                .getSuccess()
                .flatMap(element -> element.getAsObject()
                        .ifFailure(problems::add)
                        .getSuccess());
        optDescriptions.ifPresent(descObj -> {
            descObj.stream().forEach(entry -> {
                try {
                    var level = Integer.parseInt(entry.getKey());
                    var desc = entry.getValue().getAsString()
                            .ifFailure(problems::add)
                            .getSuccess();
                    desc.ifPresent(d -> levelDescriptions.put(level, d));
                } catch (NumberFormatException e) {
                    problems.add(rootObject.getPath().getObject("descriptions").getObject(entry.getKey())
                            .createProblem("Expected an integer"));
                }
            });
        });

        // Parse extra descriptions (optional)
        var levelExtraDescriptions = new HashMap<Integer, String>();
        var optExtraDescriptions = rootObject.get("extra_descriptions")
                .getSuccess()
                .flatMap(element -> element.getAsObject()
                        .ifFailure(problems::add)
                        .getSuccess());
        optExtraDescriptions.ifPresent(descObj -> {
            descObj.stream().forEach(entry -> {
                try {
                    var level = Integer.parseInt(entry.getKey());
                    var desc = entry.getValue().getAsString()
                            .ifFailure(problems::add)
                            .getSuccess();
                    desc.ifPresent(d -> levelExtraDescriptions.put(level, d));
                } catch (NumberFormatException e) {
                    problems.add(rootObject.getPath().getObject("extra_descriptions").getObject(entry.getKey())
                            .createProblem("Expected an integer"));
                }
            });
        });

        // Parse merge_description (optional, defaults to false)
        var mergeDescription = rootObject.get("merge_description")
                .getSuccess()
                .flatMap(element -> element.getAsBoolean()
                        .ifFailure(problems::add)
                        .getSuccess())
                .orElse(false);

        if (problems.isEmpty()) {
            return Result.success(new PerLevelRewardsReward(levelRewards,
                            optSkillId.orElse(null),
                            optMaxLevelTmp.orElse(Math.max(1, definedMaxLevel)),
                            optPointsPerLevel,
                            levelDescriptions,
                            levelExtraDescriptions,
                            mergeDescription,
                            requiredSkills,
                            allowPartialRewards,
                            scalingFactor));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    @Override
    public void update(RewardUpdateContext context) {
        var player = context.getPlayer();
        var uuid = player.getUuid();
        int newCount = context.getCount();
        int oldCount = counts.getOrDefault(uuid, 0);
        counts.put(uuid, newCount);

        // Check prerequisites before applying any rewards
        if (!arePrerequisitesMet(uuid)) {
            if (!allowPartialRewards) {
                // Prerequisites not met and partial rewards not allowed
                SkillLevelingMod.getInstance().getLogger().info("Prerequisites not met for skill " + (skillId != null ? skillId : "unknown") + ", skipping rewards");
                return;
            }
            // If partial rewards allowed, continue but log a warning
            SkillLevelingMod.getInstance().getLogger().warn("Prerequisites not met for skill " + (skillId != null ? skillId : "unknown") + ", but partial rewards allowed");
        }

        // Apply level rewards with enhanced logic
        for (var entry : levelRewards.entrySet()) {
            int level = entry.getKey();
            int count = newCount >= level ? 1 : 0;
            boolean action = level > oldCount && level <= newCount;

            // Apply scaling factor if configured
            if (action && scalingFactor != 1.0) {
                // Modify reward effectiveness based on scaling
                double effectiveMultiplier = Math.pow(scalingFactor, level - 1);
                SkillLevelingMod.getInstance().getLogger().debug("Applying scaling factor " + scalingFactor + " for level " + level + ", effective multiplier: " + effectiveMultiplier);
                
                // TODO: Apply scaling to rewards if supported by reward types
            }

            for (var reward : entry.getValue()) {
                try {
                    reward.instance().update(new RewardUpdateContextImpl(player, count, action));
                } catch (Exception e) {
                    SkillLevelingMod.getInstance().getLogger().error("Failed to apply reward for skill " + (skillId != null ? skillId : "unknown") + " level " + level + ": " + e.getMessage());
                    
                    if (!allowPartialRewards) {
                        // If partial rewards not allowed, stop processing on error
                        throw new RuntimeException("Reward application failed", e);
                    }
                    // Otherwise continue with next reward
                }
            }
        }
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        var disposeContext = new DisposeContext(context.getServer());
        levelRewards.values().forEach(rewardList -> {
            rewardList.forEach(rewardConfig -> {
                rewardConfig.dispose(disposeContext);
            });
        });
        counts.clear();
    }
}
