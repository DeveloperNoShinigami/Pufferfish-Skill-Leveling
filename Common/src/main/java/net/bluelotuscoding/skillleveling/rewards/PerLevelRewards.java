package net.bluelotuscoding.skillleveling.rewards;

import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.api.SkillsAPI;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A reward that implements per-level rewards based on the skill level
 */
public class PerLevelRewards implements Reward {
    
    // Use a String ID that will be converted to the appropriate type (Identifier/ResourceLocation) at registration time
    public static final String REWARD_ID = "per_level_rewards";
    
    private final String skillId;
    private final Map<Integer, List<Reward>> levelRewards;
    private final int maxLevel;
    private final int pointsPerLevel;
    private final Map<UUID, Integer> counts = new HashMap<>();
    
    public PerLevelRewards(
            String skillId, 
            Map<Integer, List<Reward>> levelRewards,
            int maxLevel,
            int pointsPerLevel) {
        this.skillId = skillId;
        this.levelRewards = levelRewards;
        this.maxLevel = maxLevel;
        this.pointsPerLevel = pointsPerLevel;
    }
    
    /**
     * Register this reward with the Skills API
     */
    public static void register() {
        // Create the identifier directly using the API to avoid mapping issues
        SkillsAPI.registerReward(SkillLevelingMod.createIdentifier(REWARD_ID), PerLevelRewards::parse);
    }
    
    /**
     * Parse the reward from configuration context
     */
    private static Result<PerLevelRewards, Problem> parse(RewardConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(obj -> {
                    return obj.get("data")
                            .andThen(JsonElement::getAsObject)
                            .andThen(data -> create(data, context));
                });
    }
    
    @Override
    public void update(RewardUpdateContext context) {
        var player = context.getPlayer();
        var uuid = player.getUuid();
        int newCount = context.getCount();
        int oldCount = counts.getOrDefault(uuid, 0);
        counts.put(uuid, newCount);
        
        for (var entry : levelRewards.entrySet()) {
            int level = entry.getKey();
            int count = newCount >= level ? 1 : 0;
            boolean action = level > oldCount && level <= newCount;
            
            for (var reward : entry.getValue()) {
                // We need to pass count and action to the reward
                // Using the original context as it may contain these parameters
                reward.update(context);
            }
        }
    }
    
    @Override
    public void dispose(RewardDisposeContext context) {
        // Dispose all level rewards
        for (List<Reward> rewards : levelRewards.values()) {
            for (Reward reward : rewards) {
                reward.dispose(context);
            }
        }
        counts.clear();
    }
    
    /**
     * Get the number of defined levels
     */
    public int getLevelCount() {
        if (levelRewards.isEmpty()) {
            return maxLevel;
        }
        return Math.max(maxLevel, levelRewards.keySet().stream().max(Integer::compareTo).orElse(0));
    }
    
    /**
     * Get the skill ID for this reward
     */
    public String getSkillId() {
        return skillId;
    }
    
    /**
     * Get the maximum level for this reward
     */
    public int getMaxLevel() {
        return maxLevel;
    }
    
    /**
     * Get the points per level for this reward
     */
    public int getPointsPerLevel() {
        return pointsPerLevel;
    }
    
    /**
     * Get the rewards for a specific level
     */
    public List<Reward> getRewardsForLevel(int level) {
        return levelRewards.getOrDefault(level, new ArrayList<>());
    }
    
    /**
     * Check if a level has rewards defined
     */
    public boolean hasLevel(int level) {
        return levelRewards.containsKey(level);
    }
    
    /**
     * Factory method to create PerLevelRewards from configuration
     */
    public static Result<PerLevelRewards, Problem> create(JsonObject config, RewardConfigContext context) {
        var problems = new ArrayList<Problem>();
        
        // Parse skill ID
        var skillIdResult = config.get("skill_id")
                .andThen(JsonElement::getAsString);
        
        if (skillIdResult.isFailure()) {
            return Result.failure(Problem.of("Expected 'skill_id' string"));
        }
        
        String skillId = skillIdResult.success();
        
        // Parse levels
        var levelsResult = config.get("levels")
                .andThen(JsonElement::getAsObject);
        
        if (levelsResult.isFailure()) {
            return Result.failure(Problem.of("Expected 'levels' object"));
        }
        
        var levelsObject = levelsResult.success();
        Map<Integer, List<Reward>> levelRewards = new HashMap<>();
        
        for (String levelStr : levelsObject.keys()) {
            try {
                int level = Integer.parseInt(levelStr);
                
                var rewardsResult = levelsObject.get(levelStr)
                        .andThen(JsonElement::getAsArray);
                
                if (rewardsResult.isFailure()) {
                    problems.add(Problem.at("levels", levelStr, "Expected array of rewards"));
                    continue;
                }
                
                var rewardsArray = rewardsResult.success();
                List<Reward> rewards = new ArrayList<>();
                
                for (int i = 0; i < rewardsArray.size(); i++) {
                    var rewardResult = context.parseReward(rewardsArray.get(i));
                    
                    if (rewardResult.isFailure()) {
                        problems.add(Problem.at("levels", levelStr, i, rewardResult.failure()));
                        continue;
                    }
                    
                    rewards.add(rewardResult.success());
                }
                
                levelRewards.put(level, rewards);
            } catch (NumberFormatException e) {
                problems.add(Problem.at("levels", levelStr, "Expected level number"));
            }
        }
        
        // Access optional fields and validate values
        var optMaxLevelTmp = config.get("max_skill_level")
                .success()
                .flatMap(element -> element.getAsInt()
                        .ifFailure(problems::add)
                        .success());
        optMaxLevelTmp.ifPresent(maxLevel -> {
            if (maxLevel < 1) {
                problems.add(Problem.at("max_skill_level", "Expected a value ≥ 1"));
            }
        });
        
        var optPointsPerLevelTmp = config.get("points_per_level")
                .success() // optional
                .flatMap(element -> element.getAsInt()
                        .ifFailure(problems::add)
                        .success());
        optPointsPerLevelTmp.ifPresent(points -> {
            if (points < 0) {
                problems.add(Problem.at("points_per_level", "Expected a value ≥ 0"));
            }
        });
        
        int definedMaxLevel = levelRewards.keySet().stream().max(Integer::compareTo).orElse(0);
        int maxLevel = optMaxLevelTmp.orElse(Math.max(1, definedMaxLevel));
        int pointsPerLevel = optPointsPerLevelTmp.orElse(0);
        
        if (!problems.isEmpty()) {
            return Result.failure(Problem.combine(problems));
        }
        
        return Result.success(new PerLevelRewards(skillId, levelRewards, maxLevel, pointsPerLevel));
    }
}
