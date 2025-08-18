package net.bluelotuscoding.skillleveling.manager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.reward.Reward;
import net.bluelotuscoding.skillleveling.data.SkillLevelingDataManager;
import net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward;
import net.bluelotuscoding.skillleveling.skills.LeveledSkill;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Manager class that extends core skill management to provide multi-level functionality
 */
public class SkillLevelingManager {
    
    private final SkillLevelingDataManager dataManager;
    private final Map<String, LeveledSkill> leveledSkills;
    private final Map<Identifier, Map<String, PerLevelRewardsReward>> perLevelRewardsRewards;
    private MinecraftServer server;
    
    public SkillLevelingManager() {
        this.dataManager = new SkillLevelingDataManager();
        this.leveledSkills = new ConcurrentHashMap<>();
        this.perLevelRewardsRewards = new ConcurrentHashMap<>();
    }
    
    public void onServerStarting(MinecraftServer server) {
        this.server = server;
        // Initialize data storage
        dataManager.initialize(server);

        // Load leveled skill configurations
        loadLeveledSkillConfigurations();
    }
    
    public void onServerReload(MinecraftServer server) {
        // Reload configurations
        loadLeveledSkillConfigurations();
    }

    public void onServerStopping(MinecraftServer server) {
        dataManager.saveAll();
    }
    
    public void onPlayerJoin(ServerPlayerEntity player) {
        // Load player skill level data
        dataManager.loadPlayerData(player);
    }
    
    public void onPlayerLeave(ServerPlayerEntity player) {
        // Save player skill level data
        dataManager.savePlayerData(player);
    }

    public Optional<MinecraftServer> getServer() {
        return Optional.ofNullable(server);
    }

    public boolean hasSkillData(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return dataManager.hasSkillLevel(player, categoryId, skillId);
    }

    public void initializeSkillData(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        dataManager.setSkillLevel(player, categoryId, skillId, 1);
    }

    public void clearSkillData(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        dataManager.clearSkillLevel(player, categoryId, skillId);
    }
    
    /**
     * Register a PerLevelRewardsReward for a specific skill
     */
    public void registerPerLevelRewardsReward(Identifier categoryId, String skillId, PerLevelRewardsReward reward) {
        perLevelRewardsRewards.computeIfAbsent(categoryId, k -> new HashMap<>())
                .put(skillId, reward);
    }
    
    /**
     * Get the PerLevelRewardsReward for a specific skill
     */
    public Optional<PerLevelRewardsReward> getPerLevelRewardsReward(Identifier categoryId, String skillId) {
        if (!perLevelRewardsRewards.containsKey(categoryId)) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(perLevelRewardsRewards.get(categoryId).get(skillId));
    }
    
    /**
     * Check if a player has unlocked a specific level of a skill
     */
    public boolean hasSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        // Check if the base skill is unlocked first using core API
        var category = SkillsAPI.getCategory(categoryId);
        if (category.isEmpty()) {
            return false;
        }
        
        // Get the skill from the category
        var skill = category.get().getSkill(skillId);
        if (skill.isEmpty() || skill.get().getState(player) != Skill.State.UNLOCKED) {
            return false;
        }
        
        // Check if the specific level is unlocked
        return dataManager.getSkillLevel(player, categoryId, skillId) >= level;
    }
    
    /**
     * Get the current level of a skill for a player
     */
    public int getSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        // Return 0 if base skill is not unlocked
        var category = SkillsAPI.getCategory(categoryId);
        if (category.isEmpty()) {
            return 0;
        }
        
        var skill = category.get().getSkill(skillId);
        if (skill.isEmpty() || skill.get().getState(player) != Skill.State.UNLOCKED) {
            return 0;
        }
        
        return dataManager.getSkillLevel(player, categoryId, skillId);
    }
    
    /**
     * Get the maximum level for a skill
     */
    public int getMaxLevel(Identifier categoryId, String skillId) {
        var reward = getPerLevelRewardsReward(categoryId, skillId);
        return reward.map(PerLevelRewardsReward::getMaxLevel).orElse(1);
    }
    
    /**
     * Get the points required for a specific level of a skill
     */
    public int getPointsForLevel(Identifier categoryId, String skillId, int level) {
        var reward = getPerLevelRewardsReward(categoryId, skillId);
        // For now, assume a fixed point cost - we can extend this in the future
        return reward.isPresent() ? 1 : 1;
    }
    
    /**
     * Get the description for a specific level of a skill
     */
    public String getDescriptionForLevel(Identifier categoryId, String skillId, int level) {
        var reward = getPerLevelRewardsReward(categoryId, skillId);
        if (reward.isPresent()) {
            return reward.get().getDescriptionForLevel(level);
        }
        return "";
    }
    
    /**
     * Get all descriptions for a skill up to a specific level, based on merge setting
     */
    public String[] getDescriptions(Identifier categoryId, String skillId, int level) {
        var reward = getPerLevelRewardsReward(categoryId, skillId);
        if (reward.isPresent()) {
            var rewardInstance = reward.get();
            if (rewardInstance.isMergeDescription() && level > 1) {
                // Return single merged description
                return new String[]{rewardInstance.getDescriptionForLevel(level)};
            } else {
                // Return individual descriptions up to the level
                var descriptions = new ArrayList<String>();
                for (int i = 1; i <= level; i++) {
                    var desc = rewardInstance.getLevelDescriptions().get(i);
                    if (desc != null && !desc.isEmpty()) {
                        descriptions.add(desc);
                    }
                }
                return descriptions.toArray(new String[0]);
            }
        }
        return new String[0];
    }
    
    /**
     * Get the extra description for a specific level of a skill
     */
    public String getExtraDescriptionForLevel(Identifier categoryId, String skillId, int level) {
        var reward = getPerLevelRewardsReward(categoryId, skillId);
        if (reward.isPresent()) {
            return reward.get().getExtraDescriptionForLevel(level);
        }
        return "";
    }
    
    /**
     * Check if descriptions should be merged for this skill
     */
    public boolean shouldMergeDescriptions(Identifier categoryId, String skillId) {
        var reward = getPerLevelRewardsReward(categoryId, skillId);
        return reward.map(PerLevelRewardsReward::isMergeDescription).orElse(false);
    }
    
    /**
     * Check if a player meets the requirements for a specific level of a skill
     */
    public boolean meetsLevelRequirements(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        // For the new reward type, we don't have level requirements
        // This could be expanded in the future if needed
        return true;
    }
    
    /**
     * Advance a skill to the next level for a player
     */
    public boolean advanceSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        var category = SkillsAPI.getCategory(categoryId);
        if (category.isEmpty()) {
            return false;
        }
        
        var skill = category.get().getSkill(skillId);
        if (skill.isEmpty() || skill.get().getState(player) != Skill.State.UNLOCKED) {
            return false;
        }
        
        int currentLevel = getSkillLevel(player, categoryId, skillId);
        int newLevel = currentLevel + 1;
        int maxLevel = getMaxLevel(categoryId, skillId);
        
        if (newLevel > maxLevel) {
            return false;
        }
        
        // Check if advancement is allowed
        if (canAdvanceToLevel(player, categoryId, skillId, newLevel)) {
            // Deduct points for this level
            if (!net.bluelotuscoding.skillleveling.points.SkillPointManager.deductPointsForLevel(player, categoryId, skillId, newLevel)) {
                return false;
            }
            
            dataManager.setSkillLevel(player, categoryId, skillId, newLevel);
            
            // Trigger rewards for the new level
            triggerLevelRewards(player, categoryId, skillId, newLevel);
            return true;
        }
        
        return false;
    }
    
    /**
     * Set a specific level for a skill for a player
     */
    public boolean setSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        var category = SkillsAPI.getCategory(categoryId);
        if (category.isEmpty()) {
            return false;
        }
        
        var skill = category.get().getSkill(skillId);
        if (skill.isEmpty()) {
            return false;
        }
        
        int maxLevel = getMaxLevel(categoryId, skillId);
        
        if (level < 0 || level > maxLevel) {
            return false;
        }
        
        int currentLevel = getSkillLevel(player, categoryId, skillId);
        
        // If we're setting a level higher than current, check requirements
        if (level > currentLevel && !canAdvanceToLevel(player, categoryId, skillId, level)) {
            return false;
        }
        
        dataManager.setSkillLevel(player, categoryId, skillId, level);
        
        // Trigger rewards for the new level if higher than current
        if (level > currentLevel) {
            triggerLevelRewards(player, categoryId, skillId, level);
        }
        
        return true;
    }
    
    private boolean canAdvanceToLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        // Check if player meets level requirements
        if (!meetsLevelRequirements(player, categoryId, skillId, level)) {
            return false;
        }
        
        // Check if player can afford the point cost
        return net.bluelotuscoding.skillleveling.points.SkillPointManager.canAffordLevel(player, categoryId, skillId, level);
    }
    
    private void triggerLevelRewards(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        // Future hook for applying per-level rewards
    }
    
    /**
     * Refund one level of a skill for a player
     */
    public boolean refundSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        var category = SkillsAPI.getCategory(categoryId);
        if (category.isEmpty()) {
            return false;
        }
        
        var skill = category.get().getSkill(skillId);
        if (skill.isEmpty() || skill.get().getState(player) != Skill.State.UNLOCKED) {
            return false;
        }
        
        int currentLevel = getSkillLevel(player, categoryId, skillId);
        
        if (currentLevel <= 1) {
            // Cannot refund below level 1 (base skill must remain unlocked)
            return false;
        }
        
        int newLevel = currentLevel - 1;
        
        // Deactivate rewards for the level being refunded
        deactivateLevelRewards(player, categoryId, skillId, currentLevel);
        
        // Refund points for this level
        net.bluelotuscoding.skillleveling.points.SkillPointManager.refundPointsForLevel(player, categoryId, skillId, currentLevel);
        
        // Set the new level
        dataManager.setSkillLevel(player, categoryId, skillId, newLevel);
        
        return true;
    }
    
    /**
     * Refund multiple levels of a skill for a player
     */
    public int refundSkillLevels(ServerPlayerEntity player, Identifier categoryId, String skillId, int count) {
        int refunded = 0;
        
        for (int i = 0; i < count; i++) {
            if (refundSkillLevel(player, categoryId, skillId)) {
                refunded++;
            } else {
                break;
            }
        }
        
        return refunded;
    }
    
    /**
     * Refund all levels of a skill for a player (except base level 1)
     */
    public int refundAllSkillLevels(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        int refunded = 0;
        
        while (refundSkillLevel(player, categoryId, skillId)) {
            refunded++;
        }
        
        return refunded;
    }
    
    private void deactivateLevelRewards(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        // Future hook for deactivating rewards when a level is refunded
    }
    
    private void loadLeveledSkillConfigurations() {
        // In a full implementation, we would load configuration from data files
        // For now, configuration is handled through datapack definitions
    }
}