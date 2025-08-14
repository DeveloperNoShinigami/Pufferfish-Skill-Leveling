package com.developernoshingami.pufferfish.skillleveling.rewards;

import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import com.developernoshingami.pufferfish.skillleveling.SkillLevelingMod;

import java.util.ArrayList;
import java.util.List;

/**
 * A reward that extends the core reward system to provide different rewards per level
 */
public class PerLevelReward implements Reward {
    
    private final List<LevelRewardConfig> levelRewards;
    
    public PerLevelReward(List<LevelRewardConfig> levelRewards) {
        this.levelRewards = levelRewards;
    }
    
    @Override
    public void update(RewardUpdateContext context) {
        var player = context.getPlayer();
        
        // Get the current skill level - this requires knowing which skill this reward belongs to
        // For now, we'll use the count from the context as the level indicator
        int skillLevel = context.getCount();
        
        // Apply rewards for all levels up to the current level
        for (var levelReward : levelRewards) {
            if (levelReward.getLevel() <= skillLevel) {
                // Apply this level's rewards
                for (var reward : levelReward.getRewards()) {
                    reward.update(context);
                }
            }
        }
    }
    
    @Override
    public void dispose(RewardDisposeContext context) {
        // Dispose all level rewards
        for (var levelReward : levelRewards) {
            for (var reward : levelReward.getRewards()) {
                reward.dispose(context);
            }
        }
    }
    
    /**
     * Factory method to create PerLevelReward from configuration
     */
    public static Result<PerLevelReward, Problem> create(JsonObject config, RewardConfigContext context) {
        var levelRewards = new ArrayList<LevelRewardConfig>();
        
        // Parse level configurations from JSON
        // This would parse a structure like:
        // {
        //   "levels": [
        //     {
        //       "level": 1,
        //       "rewards": [...]
        //     },
        //     {
        //       "level": 2,
        //       "rewards": [...]
        //     }
        //   ]
        // }
        
        // For now, create a simple implementation
        // In a full implementation, this would parse the JSON structure
        
        return Result.success(new PerLevelReward(levelRewards));
    }
    
    /**
     * Configuration for rewards at a specific level
     */
    public static class LevelRewardConfig {
        private final int level;
        private final List<Reward> rewards;
        
        public LevelRewardConfig(int level, List<Reward> rewards) {
            this.level = level;
            this.rewards = rewards;
        }
        
        public int getLevel() {
            return level;
        }
        
        public List<Reward> getRewards() {
            return rewards;
        }
    }
}