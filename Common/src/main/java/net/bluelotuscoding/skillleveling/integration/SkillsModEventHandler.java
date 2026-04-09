package net.bluelotuscoding.skillleveling.integration;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.Skill;
import net.bluelotuscoding.skillleveling.manager.SkillLevelingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ADDON INTEGRATION LAYER: Bridges the official Skills mod events with our
 * leveling system
 * 
 * This is the critical integration point that allows our addon to work
 * alongside the
 * official Skills mod without modifying its code. We listen for skill
 * unlock/lock events
 * and overlay our level progression system on top of the base skill system.
 * 
 * DESIGN PHILOSOPHY:
 * - React to Skills mod events rather than replace Skills mod functionality
 * - Maintain our own level data separate from Skills mod data
 * - Ensure our leveling system only operates on skills that are unlocked in
 * Skills mod
 * - Handle edge cases like skills unlocked before addon installation
 */
public class SkillsModEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(SkillsModEventHandler.class);
    private final SkillLevelingManager levelingManager;

    public SkillsModEventHandler(SkillLevelingManager levelingManager) {
        this.levelingManager = levelingManager;
    }

    /**
     * ADDON INITIALIZATION: Registers our event listeners with the Skills mod
     * 
     * This is called during our mod initialization to hook into the Skills mod's
     * event system. We register for skill unlock/lock events so we can manage
     * level data in response to base skill changes.
     */
    public void registerEventHandlers() {
        // SKILL UNLOCK DETECTION: When the Skills mod unlocks a skill, we initialize
        // level data
        SkillsMod.SKILL_UNLOCK.register(this::onSkillUnlock);

        // SKILL LOCK DETECTION: When the Skills mod locks a skill, we clean up level
        // data
        SkillsMod.SKILL_LOCK.register(this::onSkillLock);
    }

    /**
     * SKILL UNLOCK EVENT HANDLER: Initializes level data when a skill becomes
     * unlocked
     * 
     * CRITICAL ADDON BEHAVIOR: When the Skills mod reports a skill as unlocked,
     * we need to ensure our level data is properly initialized. This handles:
     * 
     * 1. New skill unlocks after addon installation
     * 2. Skills that were unlocked before addon was installed
     * 3. Skills that were locked then re-unlocked
     * 
     * We initialize all relevant players to level 1 for newly unlocked skills,
     * representing the "base unlock" level that the Skills mod provides.
     */
    private void onSkillUnlock(Identifier categoryId, String skillId) {
        // GET SERVER REFERENCE: We need the server to iterate through online players
        var serverOptional = levelingManager.getServer();
        if (serverOptional.isEmpty()) {
            return; // Server not ready yet
        }

        var server = serverOptional.get();

        // VERIFY SKILL EXISTS: Ensure the Skills mod actually has this skill configured
        var categoryOptional = SkillsAPI.getCategory(categoryId);
        if (categoryOptional.isEmpty()) {
            return; // Category doesn't exist, ignore event
        }

        var skillOptional = categoryOptional.get().getSkill(skillId);
        if (skillOptional.isEmpty()) {
            return; // Skill doesn't exist, ignore event
        }

        // INITIALIZE LEVEL DATA FOR ALL RELEVANT PLAYERS
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // DOUBLE-CHECK UNLOCK STATE: Only initialize if Skills mod confirms unlock
            if (skillOptional.get().getState(player) == Skill.State.UNLOCKED) {
                // CHECK IF WE ALREADY HAVE DATA: Don't overwrite existing level progress
                if (!levelingManager.hasSkillData(player, categoryId, skillId)) {
                    // INITIALIZE TO LEVEL 1: Represents the base "unlocked" state
                    levelingManager.initializeSkillData(player, categoryId, skillId);

                    // LOG FOR DEBUGGING: Helps administrators track addon behavior
                }
            }
        }
    }

    /**
     * SKILL LOCK EVENT HANDLER: Cleans up level data when a skill becomes locked
     * 
     * OPTIONAL CLEANUP BEHAVIOR: When the Skills mod locks a skill, we have
     * options:
     * 
     * 1. PRESERVE DATA: Keep level data so if skill is re-unlocked, progress
     * remains
     * 2. CLEAR DATA: Remove level data to enforce complete reset
     * 
     * Currently implementing PRESERVE DATA approach since skill locking might be
     * temporary (admin commands, temporary effects, etc.). If a more aggressive
     * cleanup is needed, this can be configured or made optional.
     */
    private void onSkillLock(Identifier categoryId, String skillId) {
        // CURRENTLY: PRESERVE DATA ON LOCK
        //
        // Design decision: We keep level data when skills are locked because:
        // 1. Skill locking might be temporary (admin actions, temporary debuffs)
        // 2. Players expect their progress to persist through temporary locks
        // 3. Level data is separate from unlock state - they can coexist safely
        //
        // If needed, this behavior can be changed or made configurable:
        //
        // AGGRESSIVE CLEANUP VERSION (commented out):
        // var serverOptional = levelingManager.getServer();
        // if (serverOptional.isEmpty()) return;
        // var server = serverOptional.get();
        // for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
        // levelingManager.clearSkillData(player, categoryId, skillId);
        // }

        // LOG THE EVENT: For debugging and administrative oversight
    }

    /**
     * MANUAL SYNCHRONIZATION: Ensures level data consistency for online players
     * 
     * This can be called by admin commands or during server maintenance to ensure
     * our level data is properly synchronized with the Skills mod's unlock state.
     * Useful for recovering from addon installation issues or data corruption.
     */
    public void synchronizeAllPlayers() {
        var serverOptional = levelingManager.getServer();
        if (serverOptional.isEmpty()) {
            return;
        }

        var server = serverOptional.get();

        // SYNC TRACKING: Count initialized entries for logging
        final int[] initializedCount = { 0 }; // Use array to make it effectively final for lambda

        // ITERATE ALL ONLINE PLAYERS
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // CHECK ALL CATEGORIES AND SKILLS
            SkillsAPI.streamCategories().forEach(category -> {
                var categoryId = category.getId();

                category.streamSkills().forEach(skill -> {
                    var skillId = skill.getId();

                    // SYNC LOGIC: Initialize missing data for unlocked skills
                    if (skill.getState(player) == Skill.State.UNLOCKED
                            && !levelingManager.hasSkillData(player, categoryId, skillId)) {
                        levelingManager.initializeSkillData(player, categoryId, skillId);
                        initializedCount[0]++;
                    }
                });
            });
        }

    }

    /**
     * PLAYER JOIN SYNCHRONIZATION: Syncs skill data when player connects
     * 
     * SYNC MECHANICS: When a player joins the server, immediately send all
     * their skill level data to their client so UI elements display correctly
     * from the moment they connect.
     */
    public void onPlayerJoin(ServerPlayerEntity player) {
        try {
            // SYNC DELAY: Small delay to ensure client is fully loaded
            var server = player.getServer();
            if (server != null) {
                server.execute(() -> {
                    // FULL SYNC: Send all skill level data to joining player
                    levelingManager.syncAllSkillsToPlayer(player);

                    // CATEGORY GATING: Initialize category lock states on join
                    net.bluelotuscoding.skillleveling.manager.CategoryLockManager.initializeLocks(player);

                    // JOIN LOGGING: Track player connections for debugging
                });
            }
        } catch (Exception e) {
            logger.error("Failed to sync skill data for joining player " + player.getName().getString() + ": "
                    + e.getMessage());
        }
    }
}
