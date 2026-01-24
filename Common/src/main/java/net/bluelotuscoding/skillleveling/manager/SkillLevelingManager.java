package net.bluelotuscoding.skillleveling.manager;

import net.bluelotuscoding.skillleveling.skills.LeveledSkill;
import net.bluelotuscoding.skillleveling.item.TomeItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.Skill;
import net.bluelotuscoding.skillleveling.data.SkillLevelingDataManager;
import net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward;
import net.bluelotuscoding.skillleveling.skills.LeveledSkill;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Manager class that extends core skill management to provide multi-level
 * functionality
 */
public class SkillLevelingManager {

    // ================================================
    // NETWORK PACKET IDENTIFIERS
    // ================================================

    /**
     * REAL-TIME SYNCHRONIZATION: Network packet types for client updates
     * 
     * SYNC MECHANICS: These packets ensure clients receive immediate updates
     * when skill levels change, maintaining UI consistency across the network.
     */
    public static final Identifier SKILL_LEVEL_UPDATE_PACKET = SkillLevelingMod.createIdentifier("skill_level_update");
    public static final Identifier SKILL_PROGRESS_UPDATE_PACKET = SkillLevelingMod
            .createIdentifier("skill_progress_update");

    private final SkillLevelingDataManager dataManager;
    private final Map<String, LeveledSkill> leveledSkills;
    private final Map<Identifier, Map<String, PerLevelRewardsReward>> perLevelRewardsRewards;
    private MinecraftServer server;
    private boolean configurationsLoaded = false;

    public SkillLevelingManager() {
        this.dataManager = new SkillLevelingDataManager();
        this.leveledSkills = new ConcurrentHashMap<>();
        this.perLevelRewardsRewards = new ConcurrentHashMap<>();
    }

    public SkillLevelingDataManager getDataManager() {
        return dataManager;
    }

    // ================================================
    // REAL-TIME CLIENT SYNCHRONIZATION
    // ================================================

    /**
     * CLIENT SYNC: Sends skill level update to player's client
     * 
     * REAL-TIME MECHANICS: Immediately notifies client of skill level changes
     * so UI elements (tooltips, progression bars, etc.) update instantly
     * without requiring screen refresh or reconnection.
     */
    public void syncSkillLevelToClient(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int currentLevel, int maxLevel) {
        syncSkillLevelToClient(player, categoryId, skillId, currentLevel, maxLevel,
                getPointsForLevel(categoryId, skillId, 1), null);
    }

    public void syncSkillLevelToClient(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int currentLevel, int maxLevel, int pointsPerLevel) {
        syncSkillLevelToClient(player, categoryId, skillId, currentLevel, maxLevel, pointsPerLevel, null);
    }

    /**
     * CLIENT SYNC: Extended version with definition ID for description mapping.
     */
    public void syncSkillLevelToClient(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int currentLevel, int maxLevel, String definitionId) {
        syncSkillLevelToClient(player, categoryId, skillId, currentLevel, maxLevel,
                getPointsForLevel(categoryId, skillId, 1), definitionId);
    }

    public void syncSkillLevelToClient(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int currentLevel, int maxLevel, int pointsPerLevel, String definitionId) {
        try {
            var networkHandler = SkillLevelingMod.getInstance().getNetworkHandler();
            if (networkHandler != null) {
                networkHandler.sendToPlayer(
                        new net.bluelotuscoding.skillleveling.network.SyncSkillLevelPacket(categoryId, skillId,
                                currentLevel, maxLevel, pointsPerLevel, definitionId),
                        player);

            }
        } catch (Exception e) {
            var logger = SkillLevelingMod.getInstance().getLogger();
            logger.error("Failed to sync skill level to client: " + e.getMessage());
        }
    }

    /**
     * DESCRIPTION SYNC: Sends per-level descriptions to client for tooltip display.
     */
    public void syncDescriptionsToClient(ServerPlayerEntity player, String definitionId,
            java.util.Map<Integer, String> levelDescriptions,
            java.util.Map<Integer, String> levelExtraDescriptions,
            boolean mergeDescription, int maxLevel) {
        try {
            var networkHandler = SkillLevelingMod.getInstance().getNetworkHandler();
            if (networkHandler != null) {
                networkHandler.sendToPlayer(
                        new net.bluelotuscoding.skillleveling.network.SyncSkillDescriptionsPacket(
                                definitionId, levelDescriptions, levelExtraDescriptions, mergeDescription, maxLevel),
                        player);
            }
        } catch (Exception e) {
            var logger = SkillLevelingMod.getInstance().getLogger();
            logger.error("Failed to sync descriptions to client: " + e.getMessage());
        }
    }

    /**
     * PLAYER PROGRESSION SYNC: Sends comprehensive skill data to joining player
     * 
     * FULL SYNC MECHANICS: When players join, send all their skill level data
     * AND descriptions to ensure client UI shows correct progression state
     * immediately.
     */
    public void syncAllSkillsToPlayer(ServerPlayerEntity player) {
        int syncedSkills = 0;

        // Iterate through all skills that have leveling enabled
        for (LeveledSkill info : new java.util.ArrayList<>(leveledSkills.values())) {
            try {
                int currentLevel = getSkillLevel(player, info.getCategoryId(), info.getSkillId());

                // Get PerLevelRewardsReward for descriptions
                var rewardOpt = getPerLevelRewardsReward(info.getCategoryId(), info.getSkillId());
                String definitionId = null;

                if (rewardOpt.isPresent()) {
                    var plr = rewardOpt.get();
                    definitionId = plr.getSkillId(); // Definition ID is often same as skill ID

                    // Sync descriptions for this skill (needed for tooltip display)
                    var levelDescs = plr.getLevelDescriptions();
                    var extraDescs = plr.getLevelExtraDescriptions();
                    boolean merge = plr.isMergeDescription();

                    if ((levelDescs != null && !levelDescs.isEmpty())
                            ||
                            (extraDescs != null && !extraDescs.isEmpty())) {
                        syncDescriptionsToClient(player, definitionId, levelDescs, extraDescs, merge,
                                plr.getMaxLevel());
                    }
                }

                // Sync level info (always, even for level 0 skills)
                syncSkillLevelToClient(player, info.getCategoryId(), info.getSkillId(),
                        currentLevel, info.getMaxLevel(), definitionId);
                syncedSkills++;
            } catch (Exception e) {
                SkillLevelingMod.getInstance().getLogger()
                        .error("Failed to sync skill " + info.getSkillId() + ": " + e.getMessage());
            }
        }

        if (syncedSkills > 0) {
            SkillLevelingMod.getInstance().getLogger()
                    .info("Synchronized " + syncedSkills + " skill levels and descriptions for player "
                            + player.getName().getString());
        }
    }

    public void onServerStarting(MinecraftServer server) {
        this.server = server;
        // Initialize data storage
        dataManager.initialize(server);
        // Note: configurations are loaded on reload or lazily on first access
    }

    public void onServerReload(MinecraftServer server) {
        this.server = server;
        // Reload leveled skill configurations after datapack reload
        loadLeveledSkillConfigurations();
    }

    public void onServerStopping(MinecraftServer server) {
        dataManager.saveAll();
    }

    public void onPlayerJoin(ServerPlayerEntity player) {
        // CRITICAL: Ensure configurations are loaded before any skill lookups
        ensureConfigurationsLoaded();

        // Load player skill level data from disk
        dataManager.loadPlayerData(player);

        // AUTO-INITIALIZATION: Ensure all currently unlocked skills have level data in
        // our manager
        // This handles skills unlocked before the addon was installed or when data is
        // missing
        for (LeveledSkill info : leveledSkills.values()) {
            if (info.getBaseSkill().getState(player) == net.puffish.skillsmod.api.Skill.State.UNLOCKED) {
                if (!hasSkillData(player, info.getCategoryId(), info.getSkillId())) {
                    initializeSkillData(player, info.getCategoryId(), info.getSkillId());
                }
            }
        }

        // Sync all skill levels and descriptions to client for UI display
        syncAllSkillsToPlayer(player);
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

    public boolean canLevelUp(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        int currentLevel = getSkillLevel(player, categoryId, skillId);
        int maxLevel = getMaxLevel(categoryId, skillId);
        return currentLevel < maxLevel;
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
     * Get all registered per-level rewards (package-private for internal use)
     */
    public Map<Identifier, Map<String, PerLevelRewardsReward>> getPerLevelRewardsRewards() {
        return perLevelRewardsRewards;
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
        var categoryOpt = SkillsAPI.getCategory(categoryId);
        if (categoryOpt.isEmpty()) {
            return 0;
        }

        var skill = categoryOpt.get().getSkill(skillId);
        if (skill.isEmpty() || skill.get().getState(player) != Skill.State.UNLOCKED) {
            return 0;
        }

        // Try to get from Mixin first as it's the most "live" during a session
        var ext = getCategoryDataExtension(player, categoryId);
        if (ext != null) {
            int level = ext.addon$getSkillLevel(skillId);
            if (level > 0) {
                return level;
            }
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
        if (reward.isPresent()) {
            return reward.get().getEffectivePointsPerLevel(level);
        }
        return 1; // Default fallback
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
     * Get all descriptions for a skill up to a specific level, based on merge
     * setting
     */
    public String[] getDescriptions(Identifier categoryId, String skillId, int level) {
        var reward = getPerLevelRewardsReward(categoryId, skillId);
        if (reward.isPresent()) {
            var rewardInstance = reward.get();
            if (rewardInstance.isMergeDescription() && level > 1) {
                // Return single merged description
                return new String[] {
                        rewardInstance.getDescriptionForLevel(level)
                };
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
     * Get all descriptions for a skill (for network/UI use)
     */
    public Map<Integer, String> getDescriptions(Identifier categoryId, String skillId) {
        var reward = getPerLevelRewardsReward(categoryId, skillId);
        if (reward.isEmpty()) {
            return Map.of();
        }
        return reward.get().getLevelDescriptions();
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
     * Get all extra descriptions for a skill (for network/UI use)
     */
    public Map<Integer, String> getExtraDescriptions(Identifier categoryId, String skillId) {
        var reward = getPerLevelRewardsReward(categoryId, skillId);
        if (reward.isEmpty()) {
            return Map.of();
        }
        return reward.get().getLevelExtraDescriptions();
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
     * Ensure configurations are loaded (lazy-load if not yet loaded)
     */
    private void ensureConfigurationsLoaded() {
        if (!configurationsLoaded && server != null) {
            loadLeveledSkillConfigurations();
        }
    }

    /**
     * Advance a skill to the next level for a player (overloaded for convenience)
     */
    public void advanceSkillLevel(ServerPlayerEntity player, String skillId) {
        // Lazy-load configurations if not yet loaded
        ensureConfigurationsLoaded();

        for (var entry : perLevelRewardsRewards.entrySet()) {
            if (entry.getValue().containsKey(skillId)) {
                advanceSkillLevel(player, entry.getKey(), skillId);
                return;
            }
        }
    }

    /**
     * Advance a skill to the next level for a player
     */
    public boolean advanceSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return advanceSkillLevel(player, categoryId, skillId, false);
    }

    /**
     * Advance a skill to the next level for a player (with optional prereq bypass)
     * 
     * @param bypassPrerequisites If true, skip unlock state and prerequisite checks
     *                            (admin use)
     *                            Points are ALWAYS deducted regardless of this
     *                            flag.
     */
    public boolean advanceSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId,
            boolean bypassPrerequisites) {
        var category = SkillsAPI.getCategory(categoryId);
        if (category.isEmpty()) {
            return false;
        }

        var skill = category.get().getSkill(skillId);
        if (skill.isEmpty()) {
            return false;
        }

        // For admin bypass, allow advancing even if skill is not unlocked yet
        if (!bypassPrerequisites && skill.get().getState(player) != Skill.State.UNLOCKED) {
            return false;
        }

        int currentLevel = getSkillLevel(player, categoryId, skillId);
        int newLevel = currentLevel + 1;
        int maxLevel = getMaxLevel(categoryId, skillId);

        if (newLevel > maxLevel) {
            return false;
        }

        // Check if advancement is allowed (skip if bypassing prerequisites)
        if (!bypassPrerequisites && !canAdvanceToLevel(player, categoryId, skillId, newLevel)) {
            return false;
        }

        // Deduct points (handled dynamically by CategoryDataMixin.getSpentPoints)
        // We just need to ensure the client is notified of the level change
        // so it recalculates its own spent points.

        dataManager.setSkillLevel(player, categoryId, skillId, newLevel);

        // Also update the Mixin data for immediate point calculation updates
        var ext = getCategoryDataExtension(player, categoryId);
        if (ext != null) {
            ext.addon$setOwner(player);
            ext.addon$setCategoryId(categoryId);
            ext.addon$setSkillLevel(skillId, newLevel);
        }

        // Trigger rewards for the new level
        triggerLevelRewards(player, categoryId, skillId, newLevel);

        // REAL-TIME SYNC: Immediately notify client of level advancement
        syncSkillLevelToClient(player, categoryId, skillId, newLevel, maxLevel);

        // Force full category sync to update client UI (points display, skill states)
        forceFullCategorySync(player, categoryId);

        return true;
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

        // Also update the Mixin data for immediate point calculation updates
        var ext = getCategoryDataExtension(player, categoryId);
        if (ext != null) {
            ext.addon$setOwner(player);
            ext.addon$setCategoryId(categoryId);
            ext.addon$setSkillLevel(skillId, level);
        }

        // Trigger rewards for ALL levels between old and new (not just the final one)
        if (level > currentLevel) {
            for (int lvl = currentLevel + 1; lvl <= level; lvl++) {
                triggerLevelRewards(player, categoryId, skillId, lvl);
            }
        } else if (level < currentLevel) {
            // Deactivate rewards for levels being removed
            for (int lvl = currentLevel; lvl > level; lvl--) {
                deactivateLevelRewards(player, categoryId, skillId, lvl);
            }
        }

        // REAL-TIME SYNC: Immediately notify client of level change
        syncSkillLevelToClient(player, categoryId, skillId, level, maxLevel);

        // Force full category sync to update client UI (points display, skill states)
        forceFullCategorySync(player, categoryId);

        return true;
    }

    private boolean canAdvanceToLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        // Check if player meets level requirements
        if (!meetsLevelRequirements(player, categoryId, skillId, level)) {
            return false;
        }

        // Check if player can afford the point cost
        return net.bluelotuscoding.skillleveling.points.SkillPointManager.canAffordLevel(player, categoryId, skillId,
                level);
    }

    private void triggerLevelRewards(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        getPerLevelRewardsReward(categoryId, skillId).ifPresent(reward -> reward
                .update(new net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl(player, level, true)));
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

        if (currentLevel <= 0) {
            // Cannot refund below level 0
            return false;
        }

        int newLevel = currentLevel - 1;

        // Deactivate rewards for the level being refunded
        deactivateLevelRewards(player, categoryId, skillId, currentLevel);

        // DO NOT MANUALLY REFUND POINTS HERE
        // The CategoryDataMixin dynamic getSpentPoints handles the refund automatically
        // when the level is decreased in the map.

        // Set the new level
        dataManager.setSkillLevel(player, categoryId, skillId, newLevel);

        // Also update the Mixin data for immediate point calculation updates
        var ext = getCategoryDataExtension(player, categoryId);
        if (ext != null) {
            ext.addon$setOwner(player);
            ext.addon$setCategoryId(categoryId);
            ext.addon$setSkillLevel(skillId, newLevel);
        }

        // REAL-TIME SYNC: Immediately notify client of level refund
        syncSkillLevelToClient(player, categoryId, skillId, newLevel,
                getMaxLevel(categoryId, skillId));

        // Force full category sync to update client UI (points display, skill states)
        // This replaces the addPoints(..., 0) hack which only synced points, not skill
        // states
        forceFullCategorySync(player, categoryId);

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
     * Refund all levels of a skill for a player (to level 0)
     */
    public int refundAllSkillLevels(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        int refunded = 0;

        while (refundSkillLevel(player, categoryId, skillId)) {
            refunded++;
        }

        return refunded;
    }

    private void deactivateLevelRewards(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        getPerLevelRewardsReward(categoryId, skillId).ifPresent(reward -> reward
                .update(new net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl(player, level - 1, false)));
    }

    /**
     * Enhanced prerequisite checking for skills with required_skill dependencies
     * 
     * @param player     The player to check
     * @param categoryId The category of the skill
     * @param skillId    The skill to check prerequisites for
     * @return true if all prerequisites are met
     */
    public boolean checkSkillPrerequisites(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return checkSkillPrerequisites(player.getUuid(), categoryId, skillId);
    }

    /**
     * Enhanced prerequisite checking for skills with required_skill dependencies
     * 
     * @param playerId   The player UUID to check
     * @param categoryId The category of the skill
     * @param skillId    The skill to check prerequisites for
     * @return true if all prerequisites are met
     */
    public boolean checkSkillPrerequisites(UUID playerId, Identifier categoryId, String skillId) {
        var reward = getPerLevelRewardsReward(categoryId, skillId);
        if (reward.isEmpty()) {
            return true; // No reward means no prerequisites
        }

        var requiredSkills = reward.get().getRequiredSkills();
        if (requiredSkills.isEmpty()) {
            return true; // No prerequisites defined
        }

        // Check each prerequisite
        for (var prerequisite : requiredSkills) {
            if (!checkSinglePrerequisite(playerId, categoryId, prerequisite)) {
                SkillLevelingMod.getInstance().getLogger().debug("Prerequisite not met for " + skillId + ": "
                        + prerequisite.getSkillId() + " level " + prerequisite.getLevel() + " required");
                return false;
            }
        }

        return true;
    }

    /**
     * Check a single skill prerequisite
     */
    private boolean checkSinglePrerequisite(UUID playerId, Identifier defaultCategoryId,
            PerLevelRewardsReward.SkillPrerequisite prerequisite) {
        // Determine category (use specified category or default to current)
        Identifier categoryId = defaultCategoryId;
        if (prerequisite.getCategoryId() != null) {
            categoryId = new Identifier(prerequisite.getCategoryId());
        }

        // Get current skill level for the prerequisite skill
        int currentLevel = getSkillLevelByUUID(playerId, categoryId, prerequisite.getSkillId());

        // Check if current level meets requirement
        boolean met = currentLevel >= prerequisite.getLevel();

        SkillLevelingMod.getInstance().getLogger().debug("Checking prerequisite " + prerequisite.getSkillId()
                + ": current level " + currentLevel + ", required level " + prerequisite.getLevel() + ", met: " + met);

        return met;
    }

    /**
     * Get skill level by UUID (for prerequisite checking)
     */
    private int getSkillLevelByUUID(UUID playerId, Identifier categoryId, String skillId) {
        // Get player by UUID
        if (server == null) {
            SkillLevelingMod.getInstance().getLogger().warn("Server not available for prerequisite checking");
            return 0;
        }

        var player = server.getPlayerManager().getPlayer(playerId);
        if (player == null) {
            SkillLevelingMod.getInstance().getLogger()
                    .debug("Player not online for prerequisite checking: " + playerId);
            return 0;
        }

        return getSkillLevel(player, categoryId, skillId);
    }

    /**
     * Get detailed prerequisite information for a skill
     */
    public List<String> getPrerequisiteInfo(Identifier categoryId, String skillId) {
        var reward = getPerLevelRewardsReward(categoryId, skillId);
        if (reward.isEmpty()) {
            return new ArrayList<>();
        }

        var requiredSkills = reward.get().getRequiredSkills();
        var info = new ArrayList<String>();

        for (var prerequisite : requiredSkills) {
            String categoryStr = prerequisite.getCategoryId() != null
                    ? prerequisite.getCategoryId()
                    : categoryId.toString();
            info.add(String.format("%s (Level %d) in %s",
                    prerequisite.getSkillId(),
                    prerequisite.getLevel(),
                    categoryStr));
        }

        return info;
    }

    /**
     * Check if a player can advance to a specific level (including prerequisites)
     */
    public boolean canAdvanceToLevelWithPrerequisites(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int targetLevel) {
        // Check basic advancement requirements
        if (!canAdvanceToLevel(player, categoryId, skillId, targetLevel)) {
            return false;
        }

        // Check prerequisites
        return checkSkillPrerequisites(player, categoryId, skillId);
    }

    @SuppressWarnings("unchecked")
    private void loadLeveledSkillConfigurations() {
        leveledSkills.clear();
        perLevelRewardsRewards.clear();
        configurationsLoaded = false; // Reset for reload

        try {
            var categoriesField = net.puffish.skillsmod.SkillsMod.class.getDeclaredField("categories");
            categoriesField.setAccessible(true);
            var changeListener = (net.puffish.skillsmod.util.ChangeListener<java.util.Optional<java.util.Map<Identifier, net.puffish.skillsmod.config.CategoryConfig>>>) categoriesField
                    .get(net.puffish.skillsmod.SkillsMod.getInstance());

            var categoriesOptional = changeListener.get();
            if (categoriesOptional == null || categoriesOptional.isEmpty()) {
                SkillLevelingMod.getInstance().getLogger()
                        .warn("No categories found in Skills mod - leveling features may be disabled");
                return;
            }

            var categories = categoriesOptional.get();
            int loadedCount = 0;

            for (var category : categories.values()) {
                Identifier categoryId = category.id();
                for (var definition : category.definitions().getAll()) {
                    String id = definition.id();

                    for (var reward : definition.rewards()) {
                        if (reward.instance() instanceof PerLevelRewardsReward perReward) {
                            registerPerLevelRewardsReward(categoryId, id, perReward);

                            final String finalId = id;
                            final Identifier finalCategoryId = categoryId;

                            var leveledConfig = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage
                                    .get(id);
                            int maxLevel = leveledConfig != null ? leveledConfig.maxLevels : perReward.getMaxLevel();

                            SkillsAPI.getCategory(categoryId)
                                    .flatMap(cat -> cat.getSkill(finalId))
                                    .ifPresent(skill -> leveledSkills.put(finalId,
                                            new LeveledSkill(skill, finalCategoryId, finalId, maxLevel)));
                            loadedCount++;
                        }
                    }
                }
            }
            configurationsLoaded = true; // Mark as loaded
            SkillLevelingMod.getInstance().getLogger()
                    .info("Successfully loaded " + loadedCount + " leveled skill configurations");
        } catch (ReflectiveOperationException e) {
            SkillLevelingMod.getInstance().getLogger()
                    .error("Failed to access internal categories via reflection: " + e.getMessage());
        }
    }

    // ================================================
    // TOME HANDLERS
    // ================================================

    /**
     * Handle Tome of Progression usage.
     * Grants enough experience to advance to the next category level.
     * Cannot be used if already at max level.
     */
    public boolean handleTomeOfProgression(ServerPlayerEntity player, Identifier categoryId) {
        try {
            var categoryOpt = net.puffish.skillsmod.api.SkillsAPI.getCategory(categoryId);
            if (categoryOpt.isEmpty()) {
                SkillLevelingMod.getInstance().getLogger()
                        .warn("Tome of Progression: Category " + categoryId + " not found");
                return false;
            }

            var category = categoryOpt.get();
            var experienceOpt = category.getExperience();

            if (experienceOpt.isEmpty()) {
                // This category doesn't use experience/levels
                SkillLevelingMod.getInstance().getLogger()
                        .warn("Tome of Progression: Category " + categoryId + " does not use experience");
                player.sendMessage(net.minecraft.text.Text.translatable(
                        "skillleveling.tome.no_experience")
                        .formatted(net.minecraft.util.Formatting.RED), false);
                return false;
            }

            var experience = experienceOpt.get();
            int currentLevel = experience.getLevel(player);
            int totalXpNow = experience.getTotal(player);

            // getRequired returns XP needed for JUST this level transition
            // getRequiredTotal returns cumulative XP for all levels up to that point
            // For Tome of Progression, we want to grant exactly enough XP for ONE level
            int xpNeeded = experience.getRequired(currentLevel + 1);

            // Check if player is at max level (getRequired returns -1 or 0 when maxed)
            if (xpNeeded <= 0) {
                SkillLevelingMod.getInstance().getLogger()
                        .info("Tome of Progression: Player " + player.getName().getString()
                                + " is already at max level for " + categoryId.getPath());
                player.sendMessage(net.minecraft.text.Text.translatable(
                        "skillleveling.tome.max_level")
                        .formatted(net.minecraft.util.Formatting.RED), false);
                return false;
            }

            // LOG XP FOR DEBUGGING
            SkillLevelingMod.getInstance().getLogger().info(
                    "[TOME DEBUG] Before: Level=" + currentLevel +
                            ", TotalXP=" + totalXpNow +
                            ", XPNeededForNext=" + xpNeeded);

            // Grant the experience
            experience.addTotal(player, xpNeeded);

            // NOTE: Do NOT call forceFullCategorySync here!
            // experience.addTotal already triggers the internal Pufferfish Skills sync,
            // and calling updateAllCategories again would cause double processing.

            // Capture and log the post-state
            int newLevel = experience.getLevel(player);
            int newTotalXp = experience.getTotal(player);
            int expectedNextLevelXp = experience.getRequiredTotal(newLevel + 1);

            SkillLevelingMod.getInstance().getLogger().info(
                    "[TOME DEBUG] After: Level=" + newLevel +
                            ", TotalXP=" + newTotalXp +
                            ", ExpectedLevel=" + (currentLevel + 1) +
                            ", NextLevelTargetXP=" + expectedNextLevelXp);

            player.sendMessage(net.minecraft.text.Text.translatable(
                    "skillleveling.tome.level_up", newLevel)
                    .formatted(net.minecraft.util.Formatting.GREEN), false);

            return true;
        } catch (Exception e) {
            SkillLevelingMod.getInstance().getLogger()
                    .error("Tome of Progression failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Handle Tome of Clear Mind usage.
     * Refunds 1 level of the selected skill and returns the point cost.
     */
    public boolean handleTomeOfClearMind(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        int currentLevel = getSkillLevel(player, categoryId, skillId);
        if (currentLevel <= 0) {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("Tome of Clear Mind: Skill " + skillId + " is already at level 0");
            return false;
        }

        int pointsToRefund = getPointsForLevel(categoryId, skillId, currentLevel);
        boolean success = refundSkillLevel(player, categoryId, skillId);
        if (success) {
            syncCategoryPoints(player, categoryId, pointsToRefund);
            SkillLevelingMod.getInstance().getLogger()
                    .info("Tome of Clear Mind: Player "
                            + player.getName().getString()
                            + " refunded 1 level of " + skillId + " (now level " + (currentLevel - 1) + ")");
        }
        return success;
    }

    /**
     * Handle Tome of Greater Clear Mind usage.
     * Resets the selected skill to level 0 and refunds all points.
     */
    public boolean handleTomeOfGreaterClearMind(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        int currentLevel = getSkillLevel(player, categoryId, skillId);
        if (currentLevel <= 0) {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("Tome of Greater Clear Mind: Skill " + skillId + " is already at level 0");
            return false;
        }

        boolean success = true;
        int totalPointsToRefund = 0;
        var reward = getPerLevelRewardsReward(categoryId, skillId);
        if (reward.isPresent()) {
            for (int i = 1; i <= currentLevel; i++) {
                totalPointsToRefund += reward.get().getEffectivePointsPerLevel(i);
            }
        } else {
            totalPointsToRefund = currentLevel;
        }

        for (int i = 0; i < currentLevel && success; i++) {
            success = refundSkillLevel(player, categoryId, skillId);
        }

        if (success) {
            syncCategoryPoints(player, categoryId, totalPointsToRefund);
            SkillLevelingMod.getInstance().getLogger()
                    .info("Tome of Greater Clear Mind: Player "
                            + player.getName().getString()
                            + " reset " + skillId + " from level " + currentLevel + " to 0");
        }

        return success;
    }

    public void syncCategoryPoints(ServerPlayerEntity player, Identifier categoryId) {
        syncCategoryPoints(player, categoryId, 0);
    }

    /**
     * Public method to trigger a point sync for a player's category.
     */
    public void syncCategoryPoints(ServerPlayerEntity player, Identifier categoryId, int pointsRefunded) {
        try {
            var categoryOpt = net.puffish.skillsmod.api.SkillsAPI.getCategory(categoryId);
            if (categoryOpt.isPresent()) {
                categoryOpt.get().addPoints(player, net.puffish.skillsmod.util.PointSources.COMMANDS, 0);

                if (pointsRefunded > 0) {
                    player.sendMessage(net.minecraft.text.Text.translatable(
                            "skillleveling.tome.points_refunded", pointsRefunded)
                            .formatted(net.minecraft.util.Formatting.GREEN), false);
                }
            }
        } catch (Exception e) {
            // Ignore sync errors
        }
    }

    /**
     * Helper to get the CategoryData extension for a player and category.
     */
    private net.bluelotuscoding.skillleveling.mixin_interface.CategoryDataExtension getCategoryDataExtension(
            ServerPlayerEntity player, Identifier categoryId) {
        try {
            var skillsMod = net.puffish.skillsmod.SkillsMod.getInstance();
            var getPlayerDataMethod = net.puffish.skillsmod.SkillsMod.class.getDeclaredMethod("getPlayerData",
                    ServerPlayerEntity.class);
            getPlayerDataMethod.setAccessible(true);
            var playerData = getPlayerDataMethod.invoke(skillsMod, player);

            if (playerData instanceof net.bluelotuscoding.skillleveling.mixin_interface.PlayerDataExtension playerExt) {
                var categoryData = playerExt.addon$getCategoryData(categoryId);
                if (categoryData instanceof net.bluelotuscoding.skillleveling.mixin_interface.CategoryDataExtension catExt) {
                    return catExt;
                }
            }
        } catch (Exception e) {
            SkillLevelingMod.getInstance().getLogger().error("Failed to get CategoryData extension: " + e.getMessage());
        }
        return null;
    }

    /**
     * Process a tome action from client request.
     * This is the central dispatcher for tome functionality.
     */
    public void processTomeAction(ServerPlayerEntity player, Identifier categoryId, String skillId,
            TomeItem.TomeType tomeType) {
        switch (tomeType) {
            case PROGRESSION:
                handleTomeOfProgression(player, categoryId);
                break;
            case CLEAR_MIND:
                handleTomeOfClearMind(player, categoryId, skillId);
                break;
            case GREATER_CLEAR_MIND:
                handleTomeOfGreaterClearMind(player, categoryId, skillId);
                break;
            default:
                SkillLevelingMod.getInstance().getLogger()
                        .warn("Unknown tome type: " + tomeType);
                break;
        }
    }

    /**
     * Force a full category sync to the client.
     * This calls SkillsMod.updateAllCategories which sends all skill states,
     * points, and other data to the client.
     */
    public void forceFullCategorySync(ServerPlayerEntity player, Identifier categoryId) {
        try {
            // Call updateAllCategories on SkillsMod to trigger full client sync
            net.puffish.skillsmod.SkillsMod.getInstance().updateAllCategories(player);

            // Also sync our addon's level data for all skills in this category
            syncAllSkillsInCategory(player, categoryId);

            SkillLevelingMod.getInstance().getLogger()
                    .debug("Forced full category sync for player " + player.getName().getString());

        } catch (Exception e) {
            SkillLevelingMod.getInstance().getLogger()
                    .error("Failed to force category sync: " + e.getMessage());
        }
    }

    /**
     * Sync all leveled skills in a category to client.
     */
    public void syncAllSkillsInCategory(ServerPlayerEntity player, Identifier categoryId) {
        try {
            var categoryOpt = net.puffish.skillsmod.api.SkillsAPI.getCategory(categoryId);
            if (categoryOpt.isEmpty())
                return;

            var category = categoryOpt.get();
            category.streamSkills().forEach(skill -> {
                String skillId = skill.getId();
                int level = getSkillLevel(player, categoryId, skillId);
                int maxLevel = getMaxLevel(categoryId, skillId);
                syncSkillLevelToClient(player, categoryId, skillId, level, maxLevel);
            });
        } catch (Exception e) {
            SkillLevelingMod.getInstance().getLogger()
                    .error("Failed to sync all skills in category: " + e.getMessage());
        }
    }
}