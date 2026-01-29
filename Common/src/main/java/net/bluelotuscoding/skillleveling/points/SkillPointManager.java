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
     * NOTE: Now handled dynamically by CategoryDataMixin.getSpentPoints.
     */
    public static boolean deductPointsForLevel(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int level) {
        return true; // Deduction is handled by dynamic getSpentPoints
    }

    /**
     * Refund points for a skill level
     * NOTE: Now handled dynamically by CategoryDataMixin.getSpentPoints.
     */
    public static void refundPointsForLevel(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int level) {
        // Refund is handled by dynamic getSpentPoints when level decreases
    }

    /**
     * Calculate total point cost for reaching a specific level
     */
    public static int calculateTotalPointCost(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int targetLevel) {
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

        int currentLevel = manager.getBaseSkillLevel(player, categoryId, skillId);
        int levelsToGain = Math.max(0, targetLevel - currentLevel);

        return levelsToGain * pointsPerLevel;
    }

    /**
     * Get current points available for a player in a category
     */
    public static int getCurrentPoints(ServerPlayerEntity player, Identifier categoryId) {
        var categoryOptional = SkillsAPI.getCategory(categoryId);
        if (categoryOptional.isEmpty()) {
            return 0;
        }

        return categoryOptional.get().getPointsLeft(player);
    }
}
