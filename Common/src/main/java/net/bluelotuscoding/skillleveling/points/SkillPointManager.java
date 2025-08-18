package net.bluelotuscoding.skillleveling.points;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

/**
 * Manages point costs and rewards for skill leveling
 */
public class SkillPointManager {
    
    /**
     * Check if a player has enough points to advance a skill level
     */
    public static boolean canAffordLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        var addon = SkillLevelingMod.getInstance();
        var manager = addon.getSkillLevelingManager();
        
        var rewardOptional = manager.getPerLevelRewardsReward(categoryId, skillId);
        if (rewardOptional.isEmpty()) {
            return true; // No point cost if no per-level reward defined
        }
        
        var reward = rewardOptional.get();
        int pointCost = reward.getPointsPerLevel();
        
        if (pointCost <= 0) {
            return true; // No point cost
        }
        
        // Check player's available points in this category
        var categoryOptional = SkillsAPI.getCategory(categoryId);
        if (categoryOptional.isEmpty()) {
            return false;
        }
        
        var category = categoryOptional.get();
        int availablePoints = category.getPointsLeft(player);
        
        return availablePoints >= pointCost;
    }
    
    /**
     * Deduct points for advancing a skill level
     */
    public static boolean deductPointsForLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        if (!canAffordLevel(player, categoryId, skillId, level)) {
            return false;
        }
        
        var addon = SkillLevelingMod.getInstance();
        var manager = addon.getSkillLevelingManager();
        
        var rewardOptional = manager.getPerLevelRewardsReward(categoryId, skillId);
        if (rewardOptional.isEmpty()) {
            return true; // No point cost
        }
        
        var reward = rewardOptional.get();
        int pointCost = reward.getPointsPerLevel();
        
        if (pointCost <= 0) {
            return true; // No point cost
        }
        
        // Deduct points from the category
        var categoryOptional = SkillsAPI.getCategory(categoryId);
        if (categoryOptional.isEmpty()) {
            return false;
        }
        
        var category = categoryOptional.get();
        // Note: This would need proper integration with the Skills mod point system
        // The actual implementation would use the Skills mod's point manipulation methods
        return true; // Placeholder for actual point deduction
    }
    
    /**
     * Refund points for a skill level
     */
    public static void refundPointsForLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        var addon = SkillLevelingMod.getInstance();
        var manager = addon.getSkillLevelingManager();
        
        var rewardOptional = manager.getPerLevelRewardsReward(categoryId, skillId);
        if (rewardOptional.isEmpty()) {
            return; // No points to refund
        }
        
        var reward = rewardOptional.get();
        int pointRefund = reward.getPointsPerLevel();
        
        if (pointRefund <= 0) {
            return; // No points to refund
        }
        
        // Add points back to the category
        var categoryOptional = SkillsAPI.getCategory(categoryId);
        if (categoryOptional.isEmpty()) {
            return;
        }
        
        var category = categoryOptional.get();
        // Note: This would need proper integration with the Skills mod point system
        // The actual implementation would use the Skills mod's point manipulation methods
    }
    
    /**
     * Calculate total point cost for reaching a specific level
     */
    public static int calculateTotalPointCost(ServerPlayerEntity player, Identifier categoryId, String skillId, int targetLevel) {
        var addon = SkillLevelingMod.getInstance();
        var manager = addon.getSkillLevelingManager();
        
        var rewardOptional = manager.getPerLevelRewardsReward(categoryId, skillId);
        if (rewardOptional.isEmpty()) {
            return 0;
        }
        
        var reward = rewardOptional.get();
        int pointsPerLevel = reward.getPointsPerLevel();
        
        if (pointsPerLevel <= 0) {
            return 0;
        }
        
        int currentLevel = manager.getSkillLevel(player, categoryId, skillId);
        int levelsToGain = Math.max(0, targetLevel - currentLevel);
        
        return levelsToGain * pointsPerLevel;
    }
}
