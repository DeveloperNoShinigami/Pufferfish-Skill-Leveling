package net.bluelotuscoding.skillleveling.manager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.Skill;
import net.bluelotuscoding.skillleveling.data.SkillLevelingDataManager;
import net.bluelotuscoding.skillleveling.skills.LeveledSkill;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager class that extends core skill management to provide multi-level functionality
 */
public class SkillLevelingManager {
    
    private final SkillLevelingDataManager dataManager;
    private final Map<String, LeveledSkill> leveledSkills;
    
    public SkillLevelingManager() {
        this.dataManager = new SkillLevelingDataManager();
        this.leveledSkills = new ConcurrentHashMap<>();
    }
    
    public void onServerStarting(MinecraftServer server) {
        // Initialize data storage
        dataManager.initialize(server);
        
        // Load leveled skill configurations
        loadLeveledSkillConfigurations();
    }
    
    public void onServerReload(MinecraftServer server) {
        // Reload configurations
        loadLeveledSkillConfigurations();
    }
    
    public void onPlayerJoin(ServerPlayerEntity player) {
        // Load player skill level data
        dataManager.loadPlayerData(player);
    }
    
    public void onPlayerLeave(ServerPlayerEntity player) {
        // Save player skill level data
        dataManager.savePlayerData(player);
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
     * Advance a skill to the next level for a player
     */
    public void advanceSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        var category = SkillsAPI.getCategory(categoryId);
        if (category.isEmpty()) {
            return;
        }
        
        var skill = category.get().getSkill(skillId);
        if (skill.isEmpty() || skill.get().getState(player) != Skill.State.UNLOCKED) {
            return;
        }
        
        int currentLevel = getSkillLevel(player, categoryId, skillId);
        int newLevel = currentLevel + 1;
        
        // Check if advancement is allowed (could add point costs, requirements, etc.)
        if (canAdvanceToLevel(player, categoryId, skillId, newLevel)) {
            dataManager.setSkillLevel(player, categoryId, skillId, newLevel);
            
            // Trigger rewards for the new level
            triggerLevelRewards(player, categoryId, skillId, newLevel);
        }
    }
    
    private boolean canAdvanceToLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        // Add logic here to check if player meets requirements for this level
        // For now, allow any advancement
        return true;
    }
    
    private void triggerLevelRewards(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        // Trigger any per-level rewards using the core reward system
        // This will be handled by our PerLevelReward class
    }
    
    private void loadLeveledSkillConfigurations() {
        // Load configuration for which skills support leveling
        // For now, assume all skills can be leveled
    }
}