package net.bluelotuscoding.skillleveling.mixin_interface;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Extension interface for CategoryData to support multi-level skills.
 * Provides methods for level tracking that shadow the original Set-based
 * storage.
 */
public interface CategoryDataExtension {
    void addon$setOwner(ServerPlayerEntity player);

    ServerPlayerEntity addon$getOwner();

    /**
     * Get the skill level for a specific skill ID.
     * Returns 0 if not unlocked, 1+ for the level.
     */
    int addon$getSkillLevel(String skillId);

    /**
     * Set the skill level for a specific skill ID.
     */
    void addon$setSkillLevel(String skillId, int level);

    /**
     * Increment the skill level by 1.
     */
    void addon$incrementSkillLevel(String skillId);

    /**
     * Decrement the skill level by 1. Returns true if skill still has levels
     * remaining.
     */
    boolean addon$decrementSkillLevel(String skillId);

    /**
     * Check if the skill has any level (> 0).
     */
    boolean addon$isSkillUnlocked(String skillId);

    /**
     * Get the max level for a skill from its definition rewards.
     */
    int addon$getMaxLevelForSkill(String skillId);

    void addon$setCategoryId(net.minecraft.util.Identifier categoryId);

    net.minecraft.util.Identifier addon$getCategoryId();
}
