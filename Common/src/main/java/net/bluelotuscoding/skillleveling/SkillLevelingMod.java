package net.bluelotuscoding.skillleveling;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.skillleveling.events.SkillLevelingEventListener;
import net.bluelotuscoding.skillleveling.manager.SkillLevelingManager;
import net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward;
import net.puffish.skillsmod.api.SkillsAPI;

/**
 * Main addon class that integrates with Pufferfish Skills to provide per-level skill rewards.
 */
public class SkillLevelingMod {
    public static final String MOD_ID = "puffish_skill_leveling";
    
    private static SkillLevelingMod instance;
    private final SkillLevelingManager skillLevelingManager;
    
    private SkillLevelingMod() {
        this.skillLevelingManager = new SkillLevelingManager();
    }
    
    public static void init() {
        instance = new SkillLevelingMod();
        
        // Register our custom reward type
        PerLevelRewardsReward.register();

        // Register server event listener if supported by the Skills API
        try {
            SkillsAPI.class
                    .getMethod("registerServerEventListener", net.puffish.skillsmod.server.event.ServerEventListener.class)
                    .invoke(null, new SkillLevelingEventListener());
        } catch (ReflectiveOperationException ignored) {
            // Older Skills API versions might not expose this method
        }
        
        // Initialize event handlers
        net.bluelotuscoding.skillleveling.events.SkillEventHandler.initialize();
    }
    
    public static SkillLevelingMod getInstance() {
        return instance;
    }
    
    public SkillLevelingManager getSkillLevelingManager() {
        return skillLevelingManager;
    }
    
    public static Identifier createIdentifier(String path) {
        return new Identifier(MOD_ID, path);
    }
    
    /**
     * Check if a player has unlocked a specific level of a skill
     */
    public boolean hasSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        return skillLevelingManager.hasSkillLevel(player, categoryId, skillId, level);
    }
    
    /**
     * Get the current level of a skill for a player
     */
    public int getSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return skillLevelingManager.getSkillLevel(player, categoryId, skillId);
    }
    
    /**
     * Advance a skill to the next level for a player
     */
    public boolean advanceSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return skillLevelingManager.advanceSkillLevel(player, categoryId, skillId);
    }
    
    /**
     * Set a specific level for a skill for a player
     */
    public boolean setSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        return skillLevelingManager.setSkillLevel(player, categoryId, skillId, level);
    }
    
    /**
     * Refund one level of a skill for a player
     */
    public boolean refundSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return skillLevelingManager.refundSkillLevel(player, categoryId, skillId);
    }
    
    /**
     * Refund multiple levels of a skill for a player
     */
    public int refundSkillLevels(ServerPlayerEntity player, Identifier categoryId, String skillId, int count) {
        return skillLevelingManager.refundSkillLevels(player, categoryId, skillId, count);
    }
    
    /**
     * Refund all levels of a skill for a player (except base level 1)
     */
    public int refundAllSkillLevels(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return skillLevelingManager.refundAllSkillLevels(player, categoryId, skillId);
    }
    
    /**
     * Get the description for a specific level of a skill
     */
    public String getDescriptionForLevel(Identifier categoryId, String skillId, int level) {
        return skillLevelingManager.getDescriptionForLevel(categoryId, skillId, level);
    }
    
    /**
     * Get the extra description for a specific level of a skill
     */
    public String getExtraDescriptionForLevel(Identifier categoryId, String skillId, int level) {
        return skillLevelingManager.getExtraDescriptionForLevel(categoryId, skillId, level);
    }
    
    /**
     * Check if descriptions should be merged for this skill
     */
    public boolean shouldMergeDescriptions(Identifier categoryId, String skillId) {
        return skillLevelingManager.shouldMergeDescriptions(categoryId, skillId);
    }
    
    /**
     * Get all descriptions for a skill up to a specific level
     */
    public String[] getDescriptions(Identifier categoryId, String skillId, int level) {
        return skillLevelingManager.getDescriptions(categoryId, skillId, level);
    }
}