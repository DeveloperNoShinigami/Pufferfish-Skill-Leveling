package net.bluelotuscoding.skillleveling.skills;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.Skill;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

/**
 * Extension of core Skill that adds multi-level progression functionality
 */
public class LeveledSkill {
    
    private final Skill baseSkill;
    private final Identifier categoryId;
    private final String skillId;
    private final int maxLevel;
    
    public LeveledSkill(Skill baseSkill, Identifier categoryId, String skillId, int maxLevel) {
        this.baseSkill = baseSkill;
        this.categoryId = categoryId;
        this.skillId = skillId;
        this.maxLevel = maxLevel;
    }
    
    /**
     * Get the base skill from the core mod
     */
    public Skill getBaseSkill() {
        return baseSkill;
    }
    
    /**
     * Get the category this skill belongs to
     */
    public Identifier getCategoryId() {
        return categoryId;
    }
    
    /**
     * Get the skill ID
     */
    public String getSkillId() {
        return skillId;
    }
    
    /**
     * Get the maximum level for this skill
     */
    public int getMaxLevel() {
        return maxLevel;
    }
    
    /**
     * Check if the base skill is unlocked for a player
     */
    public boolean isUnlocked(ServerPlayerEntity player) {
        return baseSkill.getState(player) == Skill.State.UNLOCKED;
    }
    
    /**
     * Get the current level of this skill for a player
     */
    public int getCurrentLevel(ServerPlayerEntity player) {
        if (!isUnlocked(player)) {
            return 0;
        }
        return SkillLevelingMod.getInstance().getSkillLevel(player, categoryId, skillId);
    }
    
    /**
     * Check if a player has reached a specific level of this skill
     */
    public boolean hasLevel(ServerPlayerEntity player, int level) {
        return getCurrentLevel(player) >= level;
    }
    
    /**
     * Check if a player can advance to the next level
     */
    public boolean canAdvance(ServerPlayerEntity player) {
        if (!isUnlocked(player)) {
            return false;
        }
        
        int currentLevel = getCurrentLevel(player);
        return currentLevel < maxLevel;
    }
    
    /**
     * Advance this skill to the next level for a player
     */
    public void advance(ServerPlayerEntity player) {
        if (canAdvance(player)) {
            SkillLevelingMod.getInstance().getSkillLevelingManager().advanceSkillLevel(player, categoryId, skillId);
        }
    }
    
    /**
     * Get the name of this skill
     */
    public String getName() {
        return baseSkill.getId(); // Use skill ID as the name since Skills API doesn't provide getName()
    }
    
    /**
     * Get the description of this skill
     */
    public String getDescription() {
        return "Leveled skill: " + baseSkill.getId(); // Create description since Skills API doesn't provide getDescription()
    }
}