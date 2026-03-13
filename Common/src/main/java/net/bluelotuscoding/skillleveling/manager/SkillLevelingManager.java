package net.bluelotuscoding.skillleveling.manager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.Skill;
import net.bluelotuscoding.skillleveling.data.SkillLevelingDataManager;
import net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward;
import net.bluelotuscoding.skillleveling.rewards.ToggleReward;
import net.bluelotuscoding.skillleveling.util.ImbuedSkillHelper;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.item.ItemStack;
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

    private final Map<Identifier, Map<String, PerLevelRewardsReward>> perLevelRewardsRewards;
    private final Map<Identifier, Map<String, ToggleReward>> toggleRewards;
    private MinecraftServer server;
    private boolean configurationsLoaded = false;
    private boolean networkHandlerNullWarned = false;
    private final Map<UUID, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, List<ProtectedEffect>> protectedEffects = new ConcurrentHashMap<>();
    // Track which definitionIds have been sent to which players to avoid redundant
    // sends

    public SkillLevelingManager() {
        this.dataManager = new SkillLevelingDataManager();
        this.perLevelRewardsRewards = new ConcurrentHashMap<>();
        this.toggleRewards = new ConcurrentHashMap<>();
    }

    public SkillLevelingDataManager getDataManager() {
        return dataManager;
    }

    /**
     * Set the server instance for prerequisite checking
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
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
            int baseLevel, int totalLevel, int maxLevel) {
        String definitionId = null;
        var category = net.puffish.skillsmod.api.SkillsAPI.getCategory(categoryId);
        if (category.isPresent()) {
            var skill = category.get().getSkill(skillId);
            if (skill.isPresent()) {
                // Access definitionId via reflection to be safe
                try {
                    var skillConfig = skill.get();
                    var method = skillConfig.getClass().getMethod("definitionId");
                    definitionId = (String) method.invoke(skillConfig);
                } catch (Exception e) {
                    definitionId = skillId;
                }
            }
        }
        syncSkillLevelToClient(player, categoryId, skillId, baseLevel, totalLevel, maxLevel,
                getPointsForLevel(categoryId, skillId, 1), definitionId);
    }

    public void syncSkillLevelToClient(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int baseLevel, int totalLevel, int maxLevel, int pointsPerLevel) {
        syncSkillLevelToClient(player, categoryId, skillId, baseLevel, totalLevel, maxLevel, pointsPerLevel, null);
    }

    public String getDefinitionId(Identifier categoryId, String skillId) {
        var category = net.puffish.skillsmod.api.SkillsAPI.getCategory(categoryId);
        if (category.isPresent()) {
            var skill = category.get().getSkill(skillId);
            if (skill.isPresent()) {
                try {
                    var skillConfig = skill.get();
                    var method = skillConfig.getClass().getMethod("definitionId");
                    String defId = (String) method.invoke(skillConfig);
                    if (defId != null)
                        return defId;
                } catch (Exception e) {
                    // Fallback to skillId
                }
            }
        }
        return skillId;
    }

    /**
     * CLIENT SYNC: Extended version with definition ID for description mapping.
     */
    public void syncSkillLevelToClient(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int baseLevel, int totalLevel, int maxLevel, String definitionId) {
        syncSkillLevelToClient(player, categoryId, skillId, baseLevel, totalLevel, maxLevel,
                getPointsForLevel(categoryId, skillId, 1), definitionId != null ? definitionId : skillId);
    }

    public void syncSkillLevelToClient(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int baseLevel, int totalLevel, int maxLevel, int pointsPerLevel, String definitionId) {
        try {
            var networkHandler = SkillLevelingMod.getInstance().getNetworkHandler();
            if (networkHandler != null) {
                boolean hidden = false;
                boolean toggle = false;
                int keybindSlot = 0;

                String lootMode = "";

                // Robust config lookup
                var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(definitionId);
                if (config == null) {
                    config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
                }

                if (config != null) {
                    hidden = config.hidden;
                    toggle = config.toggle;
                    keybindSlot = config.keybindSlot;
                    if (config.lootMode != null) {
                        lootMode = config.lootMode;
                    }
                }

                /*
                 * SkillLevelingMod.getInstance().getLogger().
                 * info("[syncSkillLevelToClient] Sending sync for " + skillId
                 * + " (Def: " + definitionId + ") Toggle: " + toggle);
                 */

                networkHandler.sendToPlayer(
                        new net.bluelotuscoding.skillleveling.network.SyncSkillLevelPacket(categoryId, skillId,
                                baseLevel, totalLevel, maxLevel, pointsPerLevel, definitionId, hidden, toggle,
                                keybindSlot, dataManager.isToggleActive(player, categoryId, skillId), lootMode),
                        player);

            } else {
                try {
                    if (!networkHandlerNullWarned) {
                        SkillLevelingMod.getInstance().getLogger()
                                .warn("NetworkHandler is null when attempting to sync skill level to "
                                        + player.getName().getString() + " for " + categoryId + ":" + skillId);
                        networkHandlerNullWarned = true;
                    }
                } catch (Exception ignored) {
                }
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
        int toggleLevel = findMinimumToggleLevel(definitionId);
        syncDescriptionsToClient(player, definitionId, levelDescriptions, levelExtraDescriptions, mergeDescription,
                maxLevel, toggleLevel);
    }

    public void syncDescriptionsToClient(ServerPlayerEntity player, String definitionId,
            java.util.Map<Integer, String> levelDescriptions,
            java.util.Map<Integer, String> levelExtraDescriptions,
            boolean mergeDescription, int maxLevel, int toggleLevel) {
        String lootMode = "";
        var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(definitionId);
        if (config != null && config.lootMode != null) {
            lootMode = config.lootMode;
        }

        try {
            var networkHandler = SkillLevelingMod.getInstance().getNetworkHandler();
            if (networkHandler != null) {
                /*
                 * try {
                 * SkillLevelingMod.getInstance().getLogger()
                 * .debug("[DEBUG] Preparing to send descriptions to " +
                 * player.getName().getString() + " -> "
                 * + definitionId + " (levels=" + levelDescriptions.size() + ", extras="
                 * + levelExtraDescriptions.size() + ")");
                 * } catch (Exception ignored) {
                 * }
                 * try {
                 * SkillLevelingMod.getInstance().getLogger().
                 * debug("[ADDON] Sending SyncSkillDescriptionsPacket to "
                 * + player.getName().getString() + " -> " + definitionId);
                 * } catch (Exception ignored) {
                 * }
                 */
                java.util.List<net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward.SkillPrerequisite> prereqsList = new java.util.ArrayList<>();
                if (config != null && !config.requiredSkills.isEmpty()) {
                    for (var req : config.requiredSkills) {
                        prereqsList.add(
                                new net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward.SkillPrerequisite(
                                        req.skillId, req.minLevel, req.categoryId));
                    }
                }
                // Also check rewards for advanced prerequisites
                var reward = getPerLevelRewardsRewardByDefinitionId(definitionId);
                if (reward.isPresent()) {
                    prereqsList.addAll(reward.get().getRequiredSkills());
                }

                networkHandler.sendToPlayer(
                        new net.bluelotuscoding.skillleveling.network.SyncSkillDescriptionsPacket(
                                definitionId, levelDescriptions, levelExtraDescriptions, mergeDescription, maxLevel,
                                lootMode, prereqsList, toggleLevel),
                        player);
            } else {
                if (!networkHandlerNullWarned) {
                    try {
                        SkillLevelingMod.getInstance().getLogger()
                                .warn("NetworkHandler is null when attempting to sync descriptions to "
                                        + player.getName().getString() + " for definitionId=" + definitionId);
                    } catch (Exception ignored) {
                    }
                    networkHandlerNullWarned = true;
                }
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
        /*
         * try {
         * SkillLevelingMod.getInstance().getLogger()
         * .debug("[ADDON] Starting full skill sync for player " +
         * player.getName().getString()
         * + " (registered skills="
         * + perLevelRewardsRewards.values().stream().mapToInt(m -> m.size()).sum() +
         * ")");
         * } catch (Exception ignored) {
         * }
         */

        // Phase 1: Ensure clients have all per-level descriptions first (tooltips rely
        // on these)
        for (var entry : perLevelRewardsRewards.entrySet()) {
            Identifier categoryId = entry.getKey();
            for (var skillEntry : entry.getValue().entrySet()) {
                String skillId = skillEntry.getKey();
                PerLevelRewardsReward plr = skillEntry.getValue();
                try {
                    var levelDescs = plr.getLevelDescriptions();
                    var extraDescs = plr.getLevelExtraDescriptions();
                    boolean merge = plr.isMergeDescription();

                    Map<Integer, String> levelDescsSafe = levelDescs != null ? levelDescs
                            : java.util.Collections.<Integer, String>emptyMap();
                    Map<Integer, String> extraDescsSafe = extraDescs != null ? extraDescs
                            : java.util.Collections.<Integer, String>emptyMap();

                    String definitionId = skillId;
                    try {
                        var skillsMod = net.puffish.skillsmod.SkillsMod.getInstance();
                        var getCategoryMethod = net.puffish.skillsmod.SkillsMod.class.getDeclaredMethod("getCategory",
                                Identifier.class);
                        getCategoryMethod.setAccessible(true);
                        var categoryConfigOpt = (java.util.Optional<?>) getCategoryMethod.invoke(skillsMod, categoryId);

                        if (categoryConfigOpt.isPresent()) {
                            var categoryConfig = categoryConfigOpt.get();
                            var skillsMethod = categoryConfig.getClass().getMethod("skills");
                            var skillsConfig = skillsMethod.invoke(categoryConfig);
                            var getByIdMethod = skillsConfig.getClass().getMethod("getById", String.class);
                            var skillConfigOpt = (java.util.Optional<?>) getByIdMethod.invoke(skillsConfig, skillId);

                            if (skillConfigOpt.isPresent()) {
                                var skillConfig = skillConfigOpt.get();
                                definitionId = (String) skillConfig.getClass().getMethod("definitionId")
                                        .invoke(skillConfig);
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    int toggleLevel = findMinimumToggleLevel(definitionId);

                    syncDescriptionsToClient(player, definitionId, levelDescsSafe, extraDescsSafe, merge,
                            plr.getMaxLevel(), toggleLevel);
                    if (!definitionId.equals(skillId)) {
                        syncDescriptionsToClient(player, skillId, levelDescsSafe, extraDescsSafe, merge,
                                plr.getMaxLevel());
                    }
                } catch (Exception e) {
                    SkillLevelingMod.getInstance().getLogger()
                            .error("Failed to sync descriptions for " + skillId + ": " + e.getMessage());
                }
            }
        }

        // Phase 2: Send level info for ALL skills in ALL categories (Global Sync)
        for (var category : net.puffish.skillsmod.api.SkillsAPI.streamCategories().toList()) {
            Identifier categoryId = category.getId();
            for (var skill : category.streamSkills().toList()) {
                String skillId = skill.getId();
                try {
                    int baseLevel = getBaseSkillLevel(player, categoryId, skillId);
                    int totalLevel = getTotalSkillLevel(player, categoryId, skillId);
                    int maxLevel = getMaxLevel(categoryId, skillId);

                    String definitionId = skillId;
                    try {
                        var method = skill.getClass().getMethod("definitionId");
                        definitionId = (String) method.invoke(skill);
                    } catch (Exception ignored) {
                    }

                    syncSkillLevelToClient(player, categoryId, skillId, baseLevel, totalLevel, maxLevel, definitionId);
                } catch (Exception e) {
                    SkillLevelingMod.getInstance().getLogger()
                            .error("Failed to sync skill " + skillId + ": " + e.getMessage());
                }
            }
        }

        /*
         * if (syncedSkills > 0) {
         * SkillLevelingMod.getInstance().getLogger()
         * .debug("Synchronized " + syncedSkills +
         * " skill levels and descriptions for player "
         * + player.getName().getString());
         * }
         */
    }

    public void onServerStarting(MinecraftServer server) {
        this.server = server;
        // Initialize data storage
        dataManager.initialize(server);
        // Note: configurations are loaded on reload or lazily on first access
    }

    public void onServerReload(MinecraftServer server) {
        SkillLevelingMod.getInstance().getLogger().info("[ADDON] Server Reload Detected. Reloading configurations.");
        this.server = server;

        // Clear ALL reward maps so new instances from the reloaded datapack are used.
        // Without this, stale reward instances retain old config values (e.g.
        // amplifier).
        this.configurationsLoaded = false;
        this.perLevelRewardsRewards.clear();
        this.toggleRewards.clear();
        this.protectedEffects.clear();
        net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.clear();
        net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.clearResolutionCache();

        // Re-discover configurations from the freshly-loaded datapack
        ensureConfigurationsLoaded();

        // Refresh all rewards for online players so protected effects, attributes,
        // and toggle states are rebuilt with the new reward instances.
        if (this.server != null) {
            for (ServerPlayerEntity player : this.server.getPlayerManager().getPlayerList()) {
                refreshAllRewards(player);
            }
        }
    }

    public void onServerStopping(MinecraftServer server) {
        dataManager.saveAll();
    }

    public void checkProtectedEffects(ServerPlayerEntity player, net.minecraft.entity.effect.StatusEffect type) {
        var effects = protectedEffects.get(player.getUuid());
        if (effects == null || effects.isEmpty())
            return;

        for (ProtectedEffect pe : effects) {
            net.minecraft.entity.effect.StatusEffect effect = net.minecraft.registry.Registries.STATUS_EFFECT
                    .get(pe.effectId);
            if (effect == null)
                continue;

            // If we are checking a specific type, skip others
            if (type != null && effect != type) {
                continue;
            }

            var current = player.getStatusEffect(effect);
            boolean shouldReapply = false;

            if (current == null) {
                shouldReapply = true;
            } else {
                // SMART OVERWRITE LOGIC:
                // 1. If existing effect is weaker, overwrite it.
                // 2. If existing effect is same strength but about to expire, refresh it.
                // 3. If existing effect is stronger, do nothing (respect potions).
                if (current.getAmplifier() < pe.amplifier) {
                    shouldReapply = true;
                } else if (current.getAmplifier() == pe.amplifier && current.getDuration() < 100) {
                    shouldReapply = true;
                }
            }

            if (shouldReapply) {
                int effectiveDuration = pe.duration == -1 ? Integer.MAX_VALUE : pe.duration;
                var instance = new net.minecraft.entity.effect.StatusEffectInstance(effect, effectiveDuration,
                        pe.amplifier, pe.ambient, pe.showParticles, pe.showIcon);
                if (pe.persistent) {
                    net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getPlatform()
                            .makePersistent(instance);
                }
                player.addStatusEffect(instance);
            }
        }
    }

    public void onPlayerJoin(ServerPlayerEntity player) {
        // Ensure configurations are loaded before any skill lookups
        ensureConfigurationsLoaded();

        // Load player skill level data from disk
        dataManager.loadPlayerData(player);

        // PRE-SEED REWARD COUNTS: Initialize each reward's internal count tracker with
        // the player's current level BEFORE refreshing. This prevents commands from
        // re-triggering on world rejoin implies by SkillsMod sync.
        for (var entry : perLevelRewardsRewards.entrySet()) {
            initializeRewardsForCategory(player, entry.getKey());
        }

        // PRE-SEED TOGGLE REWARD COUNTS: Initialize each reward's internal tracker.
        // We initialize with the PERSISTED state to ensure UI and effects match,
        // but we pass action=false to prevent commands from re-firing on join.
        for (var entry : toggleRewards.entrySet()) {
            Identifier categoryId = entry.getKey();
            for (var skillEntry : entry.getValue().entrySet()) {
                String skillId = skillEntry.getKey();
                ToggleReward tr = skillEntry.getValue();

                // Get authoritative state from DataManager (loaded from NBT)
                boolean isActive = dataManager.isToggleActive(player, categoryId, skillId);
                int totalLevel = getTotalSkillLevel(player, categoryId, skillId);

                // Initialize internal state to match persisted state
                tr.initializeState(player.getUuid(), isActive);

                // Update with persisted count but action=false (prevents "Enabled" message on
                // join)
                int rewardCount = isActive ? Math.max(1, totalLevel) : 0;
                tr.update(new net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl(player, rewardCount, false));
            }
        }

        // AUTO-INITIALIZATION: Ensure all currently unlocked skills have level data in
        // our manager
        try {
            for (var entry : perLevelRewardsRewards.entrySet()) {
                Identifier categoryId = entry.getKey();
                net.puffish.skillsmod.api.SkillsAPI.getCategory(categoryId).ifPresent(category -> {
                    for (String skillId : entry.getValue().keySet()) {
                        category.getSkill(skillId).ifPresent(skill -> {
                            if (skill.getState(player) == net.puffish.skillsmod.api.Skill.State.UNLOCKED) {
                                // Check if this is an actual unlock or just an equipment bonus
                                int equipmentBonus = calculateEquipmentBonus(player, categoryId, skillId);
                                if (equipmentBonus == 0) {
                                    if (!hasSkillData(player, categoryId, skillId)) {
                                        initializeSkillData(player, categoryId, skillId);
                                    }
                                }
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            SkillLevelingMod.getInstance().getLogger()
                    .error("Error during onPlayerJoin auto-initialization: " + e.getMessage());
        }

        // CATEGORY GATING: Lock categories whose prerequisites are not met BEFORE
        // syncing.
        // Must happen before syncAllSkillsToPlayer so the client sees correct lock
        // state immediately.
        net.bluelotuscoding.skillleveling.manager.CategoryLockManager.initializeLocks(player);

        // Sync all skill levels and descriptions to client for UI display
        // This triggers SkillsMod updates, so rewards must be pre-seeded by now!
        syncAllSkillsToPlayer(player);

        // REFRESH REWARDS: Ensure imbued bonuses apply their attribute modifiers etc.
        // Now that counts are pre-seeded, this won't re-trigger commands.
        refreshAllRewards(player);
    }

    public void onPlayerLeave(ServerPlayerEntity player) {
        // Save player skill level data
        dataManager.savePlayerData(player);
        protectedEffects.remove(player.getUuid());
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
        
        // SYNC TO CLIENT: Immediately inform client of the new level 1 initialization
        // so passive unlocks show up in UI without log-out.
        int totalLevel = getTotalSkillLevel(player, categoryId, skillId);
        int maxLevel = getMaxLevel(categoryId, skillId);
        syncSkillLevelToClient(player, categoryId, skillId, 1, totalLevel, maxLevel, skillId);
    }

    /**
     * Initialize counts for all per-level rewards in a specific category.
     * This is called during NBT loading to ensure we are ready for refreshes.
     */
    public void initializeRewardsForCategory(ServerPlayerEntity player, Identifier categoryId) {
        var rewards = perLevelRewardsRewards.get(categoryId);
        if (rewards != null) {
            for (var entry : rewards.entrySet()) {
                String skillId = entry.getKey();
                PerLevelRewardsReward plr = entry.getValue();
                int totalLevel = getTotalSkillLevel(player, categoryId, skillId);

                int initLevel = totalLevel;
                // NESTED TOGGLE FIX: If the reward is nested, and the toggle is inactive,
                // we initialize with count 0 to prevent the activation state from being
                // "burned".
                if (plr.isNested()) {
                    boolean isActive = dataManager.isToggleActive(player, categoryId, skillId);
                    if (!isActive) {
                        initLevel = 0;
                    }
                }

                plr.initializeCount(player.getUuid(), initLevel);
            }
        }
    }

    public void clearSkillData(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        dataManager.clearSkillLevel(player, categoryId, skillId);
    }

    /**
     * Wipes all skill data for a player and resets their NBT to empty.
     */
    public void clearAllData(ServerPlayerEntity player) {
        dataManager.clearAllData(player);
        // Also clear internal caches
        playerCooldowns.remove(player.getUuid());
        protectedEffects.remove(player.getUuid());
        // Sync to client to wipe UI
        syncAllSkillsToPlayer(player);
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
        var catMap = findCategoryRewards(categoryId);
        if (catMap != null && catMap.containsKey(skillId)) {
            return Optional.of(catMap.get(skillId));
        }

        // Lazy load and try again
        ensureConfigurationsLoaded();
        catMap = findCategoryRewards(categoryId);
        if (catMap != null) {
            return Optional.ofNullable(catMap.get(skillId));
        }
        return Optional.empty();
    }

    /**
     * Get the PerLevelRewardsReward for a specific skill definition ID.
     * Searches across all categories.
     */
    public Optional<PerLevelRewardsReward> getPerLevelRewardsRewardByDefinitionId(String definitionId) {
        if (definitionId == null) {
            return Optional.empty();
        }

        for (var entry : perLevelRewardsRewards.entrySet()) {
            var reward = entry.getValue().get(definitionId);
            if (reward != null) {
                return Optional.of(reward);
            }
        }

        // Try lazy load
        ensureConfigurationsLoaded();
        for (var entry : perLevelRewardsRewards.entrySet()) {
            var reward = entry.getValue().get(definitionId);
            if (reward != null) {
                return Optional.of(reward);
            }
        }

        return Optional.empty();
    }

    /**
     * Robust category lookup with namespace fallback for configurations.
     */
    private Map<String, PerLevelRewardsReward> findCategoryRewards(Identifier categoryId) {
        if (categoryId == null)
            return null;

        // Direct hit
        var catMap = perLevelRewardsRewards.get(categoryId);
        if (catMap != null)
            return catMap;

        // Fallback: search by path (CASE-INSENSITIVE)
        for (var entry : perLevelRewardsRewards.entrySet()) {
            if (entry.getKey().getPath().equalsIgnoreCase(categoryId.getPath())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void registerProtectedEffect(UUID playerId, ProtectedEffect effect) {
        protectedEffects.computeIfAbsent(playerId, k -> new ArrayList<>())
                .removeIf(e -> e.effectId.equals(effect.effectId));
        protectedEffects.get(playerId).add(effect);
    }

    public void unregisterProtectedEffect(UUID playerId, Identifier effectId) {
        var effects = protectedEffects.get(playerId);
        if (effects != null) {
            effects.removeIf(e -> e.effectId.equals(effectId));
        }
    }

    public static record ProtectedEffect(Identifier effectId, int amplifier, int duration, boolean ambient,
            boolean showParticles, boolean showIcon, boolean persistent) {
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
        if (skill.isEmpty()) {
            return false;
        }
        Skill.State state = skill.get().getState(player);
        if (state != Skill.State.UNLOCKED && state != Skill.State.AFFORDABLE && state != Skill.State.AVAILABLE) {
            return false;
        }

        // Check if the specific level is unlocked
        return dataManager.getSkillLevel(player, categoryId, skillId) >= level;
    }

    /**
     * Get the TOTAL skill level (Base + Equipment Bonus)
     */
    public int getTotalSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        int baseLevel = getBaseSkillLevel(player, categoryId, skillId);

        int gearBonus = calculateGearBonus(player, categoryId, skillId);
        int curioBonus = calculateCurioBonus(player, categoryId, skillId);

        // NOTE: Gating for equipment imbuing (require_unlock_for_imbuing) and
        // curio imbuing (require_unlock_for_curio_imbuing) are planned for a
        // future config update. Currently, all imbued bonuses apply unconditionally.

        int bonusLevel = gearBonus + curioBonus;
        int total = baseLevel + bonusLevel;

        return total;
    }

    /**
     * Get only the base (purchased/persisted) skill level.
     */
    public int getBaseSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        // NORMALIZE: Ensure we use the canonical namespaced ID from the tree
        String canonicalId = getCanonicalSkillId(categoryId, skillId);

        // Try to get from Mixin first as it's the most "live" during a session
        var ext = getCategoryDataExtension(player, categoryId);
        if (ext != null) {
            int level = ext.addon$getSkillLevel(canonicalId);
            if (level > 0)
                return level;
            // Also try fuzzy if level is 0
            if (!canonicalId.equals(skillId)) {
                level = ext.addon$getSkillLevel(skillId);
                if (level > 0)
                    return level;
            }
        }

        // FALLBACK: always check persisted storage
        int level = dataManager.getSkillLevel(player, categoryId, canonicalId);
        if (level > 0)
            return level;

        // Final fallback: try the raw ID in case it wasn't canonicalized in NBT
        if (!canonicalId.equals(skillId)) {
            level = dataManager.getSkillLevel(player, categoryId, skillId);
        }
        return level;
    }

    /**
     * @deprecated Use {@link #getTotalSkillLevel} or {@link #getBaseSkillLevel}
     */
    @Deprecated
    public int getSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return getTotalSkillLevel(player, categoryId, skillId);
    }

    /**
     * Calculate additional skill levels granted by all equipped items.
     */
    public int calculateEquipmentBonus(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return calculateGearBonus(player, categoryId, skillId) + calculateCurioBonus(player, categoryId, skillId);
    }

    /**
     * Calculate bonuses specifically from regular inventory/armor slots.
     */
    public int calculateGearBonus(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        int bonus = 0;
        List<ItemStack> stacks = new ArrayList<>();

        // Strictly EQUIPPED items only
        player.getInventory().armor.forEach(stacks::add);
        player.getInventory().offHand.forEach(stacks::add);
        stacks.add(player.getMainHandStack());

        for (ItemStack stack : stacks) {
            int itemBonus = getBonusFromItem(stack, categoryId, skillId, "Gear");
            if (itemBonus > 0) {
                bonus += itemBonus;
            }
        }
        return bonus;
    }

    /**
     * Calculate bonuses specifically from Curio/Extra slots.
     */
    public int calculateCurioBonus(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        int bonus = 0;
        var scanner = SkillLevelingMod.getInstance().getEquipmentScanner();
        if (scanner != null) {
            List<ItemStack> extra = scanner.getExtraEquipment(player);
            if (extra != null && !extra.isEmpty()) {
                for (ItemStack stack : extra) {
                    bonus += getBonusFromItem(stack, categoryId, skillId, "Curio");
                }
            }
        }
        return bonus;
    }

    /**
     * Helper to extract bonus from a single item stack.
     */
    private int getBonusFromItem(ItemStack stack, Identifier categoryId, String skillId, String source) {
        if (stack.isEmpty())
            return 0;

        int bonus = 0;
        List<ImbuedSkillHelper.ImbuedSkill> imbuedSkills = ImbuedSkillHelper.getSkills(stack);
        for (ImbuedSkillHelper.ImbuedSkill imbued : imbuedSkills) {
            if (isFuzzySkillMatch(imbued.skillId, skillId)) {
                // Check if category matches (flexible)
                if (categoryId == null || categoryId.getPath().equals(imbued.categoryId)
                        || categoryId.toString().equals(imbued.categoryId)) {
                    bonus += imbued.level;
                }
            }
        }
        return bonus;
    }

    /**
     * FUZZY MATCHING: Compares two skill IDs for equality, ignoring namespaces if
     * necessary.
     * 
     * RATIONALE: Tree skills are often registered with namespaces (e.g.,
     * 'template:vitality'),
     * while Skills Tomes may only store the path ('vitality'). This helper ensures
     * consistency.
     */
    private boolean isFuzzySkillMatch(String id1, String id2) {
        if (id1 == null || id2 == null)
            return false;
        if (id1.equals(id2))
            return true;

        // Path-only match for namespace flexibility
        net.minecraft.util.Identifier ident1 = net.minecraft.util.Identifier.tryParse(id1);
        net.minecraft.util.Identifier ident2 = net.minecraft.util.Identifier.tryParse(id2);

        if (ident1 != null && ident2 != null) {
            return ident1.getPath().equals(ident2.getPath());
        }

        // Fallback for non-identifier strings
        String path1 = id1.contains(":") ? id1.substring(id1.indexOf(":") + 1) : id1;
        String path2 = id2.contains(":") ? id2.substring(id2.indexOf(":") + 1) : id2;

        return path1.equals(path2);
    }

    public String getCanonicalSkillId(Identifier categoryId, String skillId) {
        if (skillId == null)
            return null;

        // If it already contains a colon, it's likely already canonical or at least
        // namespaced
        if (skillId.contains(":")) {
            return skillId;
        }

        var categoryRewards = perLevelRewardsRewards.get(categoryId);
        if (categoryRewards == null) {
            // Try fuzzy category lookup
            var catMap = findCategoryRewards(categoryId);
            if (catMap != null) {
                categoryRewards = catMap;
            }
        }

        if (categoryRewards != null) {
            // 1. Try exact path match
            for (String registeredId : categoryRewards.keySet()) {
                if (isFuzzySkillMatch(registeredId, skillId)) {
                    return registeredId;
                }
            }
        }

        // 2. Fallback: Search across ALL categories if not found in current
        // (cross-category support)
        for (var entry : perLevelRewardsRewards.entrySet()) {
            for (String registeredId : entry.getValue().keySet()) {
                if (isFuzzySkillMatch(registeredId, skillId)) {
                    return registeredId;
                }
            }
        }

        return skillId; // Return as-is if no canonical match found
    }

    /**
     * Get the maximum level for a skill
     */
    public int getMaxLevel(Identifier categoryId, String skillId) {
        var reward = getPerLevelRewardsReward(categoryId, skillId);
        if (reward.isPresent()) {
            return reward.get().getMaxLevel();
        }

        // Fallback to LeveledConfigStorage which covers skills without
        // per_level_rewards
        var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
        if (config != null) {
            return config.maxLevels;
        }

        return 1;
    }

    /**
     * Scans the reward tree to find the minimum level that introduces a toggle
     * component.
     * Used for the "Ready at Level X" tooltip.
     */
    public int findMinimumToggleLevel(String definitionId) {
        var plrOpt = getPerLevelRewardsRewardByDefinitionId(definitionId);
        if (plrOpt.isEmpty()) {
            // Check if it's a global toggle at root
            var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(definitionId);
            if (config != null && config.toggle) {
                // If it's a root ToggleReward, the toggle is available at level 1 (or 0 for
                // loot).
                // However, we return 0 if it's already "global" so the mixin shows standard
                // "READY".
                return 0;
            }
            return 0;
        }

        PerLevelRewardsReward plr = plrOpt.get();
        // Hybrid skills: Check each level for a ToggleReward
        for (int level = 1; level <= plr.getMaxLevel(); level++) {
            var rewards = plr.getLevelRewards().get(level);
            if (rewards != null) {
                for (var r : rewards) {
                    if (r.instance() instanceof ToggleReward) {
                        return level;
                    }
                }
            }
        }
        return 0;
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
    public void ensureConfigurationsLoaded() {
        if (configurationsLoaded) {
            return;
        }

        SkillLevelingMod.getInstance().getLogger().info("[ADDON] [CONFIG LOAD] Starting configuration discovery...");

        try {
            var mod = net.puffish.skillsmod.SkillsMod.getInstance();
            if (mod == null) {
                SkillLevelingMod.getInstance().getLogger().error("[CONFIG LOAD] SkillsMod instance is null!");
                return;
            }

            // Discovery loop...

            for (var category : net.puffish.skillsmod.api.SkillsAPI.streamCategories().toList()) {
                try {
                    Identifier categoryId = category.getId();

                    var getCategoryMethod = net.puffish.skillsmod.SkillsMod.class.getDeclaredMethod("getCategory",
                            Identifier.class);
                    getCategoryMethod.setAccessible(true);
                    var categoryConfigOpt = (java.util.Optional<?>) getCategoryMethod.invoke(mod, categoryId);

                    if (categoryConfigOpt.isPresent()) {
                        var categoryConfig = categoryConfigOpt.get();

                        // Access internal configuration structures via reflection to avoid API
                        // limitations
                        var skillsMethod = categoryConfig.getClass().getMethod("skills");
                        var skillsConfig = skillsMethod.invoke(categoryConfig);

                        var definitionsMethod = categoryConfig.getClass().getMethod("definitions");
                        var definitionsConfig = definitionsMethod.invoke(categoryConfig);

                        for (var skill : category.streamSkills().toList()) {
                            try {
                                String skillId = skill.getId();
                                var skillConfigOpt = (java.util.Optional<?>) skillsConfig.getClass()
                                        .getMethod("getById", String.class).invoke(skillsConfig, skillId);

                                if (skillConfigOpt.isPresent()) {
                                    var skillConfig = skillConfigOpt.get();
                                    String defId = (String) skillConfig.getClass().getMethod("definitionId")
                                            .invoke(skillConfig);

                                    var defOpt = (java.util.Optional<?>) definitionsConfig.getClass()
                                            .getMethod("getById", String.class).invoke(definitionsConfig, defId);
                                    if (defOpt.isPresent()) {
                                        var definition = defOpt.get();
                                        var rewards = (java.util.Collection<?>) definition.getClass()
                                                .getMethod("rewards").invoke(definition);

                                        for (Object rewardObj : rewards) {
                                            try {
                                                var rewardInstance = rewardObj.getClass().getMethod("instance")
                                                        .invoke(rewardObj);
                                                if (rewardInstance instanceof net.puffish.skillsmod.api.reward.Reward r) {
                                                    registerRewardRecursive(categoryId, skillId, r);
                                                }
                                            } catch (Exception e) {
                                                SkillLevelingMod.getInstance().getLogger()
                                                        .error("Failed to process reward: " + e.getMessage());
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                SkillLevelingMod.getInstance().getLogger()
                                        .error("[CONFIG LOAD] Failed to process skill: " + skill.getId() + " - "
                                                + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    SkillLevelingMod.getInstance().getLogger()
                            .error("[CONFIG LOAD] Failed to process category: " + e.getMessage());
                }
            }

            /*
             * SkillLevelingMod.getInstance().getLogger().
             * debug("[CONFIG LOAD] Discovery complete: " + categoryCount
             * + " categories, " + skillCount + " skills registered");
             */
            configurationsLoaded = true;
            // Sync to all online players now that configs are loaded
            if (this.server != null) {
                for (ServerPlayerEntity player : this.server.getPlayerManager().getPlayerList()) {
                    syncAllSkillsToPlayer(player);
                }
            }
        } catch (Exception e) {
            SkillLevelingMod.getInstance().getLogger()
                    .error("Failed to discover leveled skill configurations: " + e.getMessage());
            e.printStackTrace();
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
     * Advance a skill to the next level for a player (overloaded for convenience)
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
        // Lazy-load configurations if not yet loaded
        ensureConfigurationsLoaded();

        // NORMALIZE: Ensure IDs from NBT/Command match registered registry IDs
        categoryId = normalizeCategoryId(categoryId);
        skillId = getCanonicalSkillId(categoryId, skillId);

        var category = SkillsAPI.getCategory(categoryId);
        if (category.isEmpty()) {
            SkillLevelingMod.getInstance().getLogger().warn("[TOME DEBUG] Category not found: " + categoryId);
            return false;
        }

        var skill = category.get().getSkill(skillId);
        if (skill.isEmpty()) {
            return false;
        }

        // For admin bypass, allow advancing even if skill is not unlocked yet
        if (!bypassPrerequisites && skill.get().getState(player) != Skill.State.UNLOCKED) {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("Cannot advance " + skillId + ": Skill not unlocked and not bypassing");
            return false;
        }

        // Check for prerequisites and affordability based on BASE level
        int currentBaseLevel = getBaseSkillLevel(player, categoryId, skillId);
        int newBaseLevel = currentBaseLevel + 1;
        int maxLevel = getMaxLevel(categoryId, skillId);

        /*
         * SkillLevelingMod.getInstance().getLogger().debug("[TOME DEBUG] Advancing " +
         * skillId + ": currentBase="
         * + currentBaseLevel + ", newBase=" + newBaseLevel + ", max=" + maxLevel +
         * ", bypass="
         * + bypassPrerequisites);
         */

        if (newBaseLevel > maxLevel) {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("Cannot advance " + skillId + ": already at max level " + maxLevel);
            return false;
        }

        // Check if advancement is allowed (skip if bypassing prerequisites)
        if (!bypassPrerequisites && !canAdvanceToLevel(player, categoryId, skillId, newBaseLevel, false)) {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("Cannot advance " + skillId + ": requirements not met and not bypassing");
            return false;
        }

        // Deduct points (handled dynamically by CategoryDataMixin.getSpentPoints)
        // We just need to ensure the client is notified of the level change
        // so it recalculates its own spent points.

        dataManager.setSkillLevel(player, categoryId, skillId, newBaseLevel);

        // Also update the Mixin data for immediate point calculation updates
        var ext = getCategoryDataExtension(player, categoryId);
        if (ext != null) {
            ext.addon$setOwner(player);
            ext.addon$setCategoryId(categoryId);

            // Mark the new level as PAID since advanceSkillLevel always deducts points
            long bits = ext.addon$getPaidLevels(skillId);
            ext.addon$setPaidLevels(skillId, bits | (1L << newBaseLevel));

            ext.addon$setSkillLevel(skillId, newBaseLevel);
        }

        // Trigger rewards for the new level
        triggerLevelRewards(player, categoryId, skillId, newBaseLevel);

        // REAL-TIME SYNC: Immediately notify client of level advancement
        // Sync TOTAL level to client for UI
        int baseLevel = getBaseSkillLevel(player, categoryId, skillId);
        int totalLevel = getSkillLevel(player, categoryId, skillId);
        syncSkillLevelToClient(player, categoryId, skillId, baseLevel, totalLevel, maxLevel);

        // Ensure the core Skills mod updates its UI/state (points/spent calculation)
        try {
            net.puffish.skillsmod.SkillsMod.getInstance().updateAllCategories(player);
        } catch (Exception ignored) {
        }

        // Sync our category points to the client as well
        try {
            syncCategoryPoints(player, categoryId);
        } catch (Exception ignored) {
        }

        return true;
    }

    /**
     * Set a specific level for a skill for a player
     */
    public boolean setSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        return setSkillLevel(player, categoryId, skillId, level, false);
    }

    /**
     * Set a specific level for a skill for a player with optional point bypass.
     */
    public boolean setSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level,
            boolean bypassPoints) {
        // NORMALIZE: Ensure IDs from NBT/Command match registered registry IDs
        categoryId = normalizeCategoryId(categoryId);
        skillId = getCanonicalSkillId(categoryId, skillId);

        var category = SkillsAPI.getCategory(categoryId);
        if (category.isEmpty()) {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("[setSkillLevel] FAILED: Category not found: " + categoryId);
            return false;
        }

        var skill = category.get().getSkill(skillId);
        if (skill.isEmpty()) {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("[setSkillLevel] FAILED: Skill not found: " + skillId + " in category " + categoryId);
            return false;
        }

        int maxLevel = getMaxLevel(categoryId, skillId);

        if (level < 0 || level > maxLevel) {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("[setSkillLevel] FAILED: Level " + level + " out of bounds (max=" + maxLevel + ")");
            return false;
        }

        int currentBaseLevel = getBaseSkillLevel(player, categoryId, skillId);

        // If we're setting a level higher than current, check requirements
        /*
         * if (level > currentBaseLevel && !canAdvanceToLevel(player, categoryId,
         * skillId, level, bypassPoints)) {
         * SkillLevelingMod.getInstance().getLogger()
         * .warn("[setSkillLevel] FAILED: canAdvanceToLevel returned false for " +
         * skillId);
         * return false;
         * }
         */
        if (level > currentBaseLevel && !canAdvanceToLevel(player, categoryId, skillId, level, bypassPoints)) {
            return false;
        }

        dataManager.setSkillLevel(player, categoryId, skillId, level);

        // Also update the Mixin data for immediate point calculation updates
        var ext = getCategoryDataExtension(player, categoryId);
        if (ext != null) {
            ext.addon$setOwner(player);
            ext.addon$setCategoryId(categoryId);

            if (!bypassPoints) {
                // If not bypassing, mark all levels from current to new as PAID
                long bits = ext.addon$getPaidLevels(skillId);
                for (int lvl = currentBaseLevel + 1; lvl <= level; lvl++) {
                    bits |= (1L << lvl);
                }
                ext.addon$setPaidLevels(skillId, bits);
            }

            ext.addon$setSkillLevel(skillId, level);
        }

        // Trigger rewards for ALL levels between old and new (not just the final one)
        if (level > currentBaseLevel) {
            for (int lvl = currentBaseLevel + 1; lvl <= level; lvl++) {
                triggerLevelRewards(player, categoryId, skillId, lvl);
            }
        } else if (level < currentBaseLevel) {
            // Deactivate rewards for levels being removed
            for (int lvl = currentBaseLevel; lvl > level; lvl--) {
                deactivateLevelRewards(player, categoryId, skillId, lvl);
            }
        }

        // REAL-TIME SYNC: Immediately notify client of level change.
        // SYNC FIX: Perform a full sync instead of single skill sync.
        // This ensures that prerequisites for OTHER skills are refreshed immediately,
        // correctly updating successor states in the UI when a requirement is met.
        syncAllSkillsToPlayer(player);

        // Ensure the core Skills mod updates its UI/state (points/spent calculation)
        try {
            net.puffish.skillsmod.SkillsMod.getInstance().updateAllCategories(player);
        } catch (Exception ignored) {
        }

        // Sync our category points to the client as well
        try {
            syncCategoryPoints(player, categoryId);
        } catch (Exception ignored) {
        }

        // removed forceFullCategorySync to prevent stuttering

        return true;
    }

    /**
     * REWARD REFRESH: Re-applies all skill rewards for a player.
     * Use this when equipment changes or when sync issues occur.
     */
    public void refreshAllRewards(ServerPlayerEntity player) {
        for (var entry : perLevelRewardsRewards.entrySet()) {
            Identifier categoryId = entry.getKey();
            for (var skillEntry : entry.getValue().entrySet()) {
                String skillId = skillEntry.getKey();
                PerLevelRewardsReward plr = skillEntry.getValue();

                // SKIP NESTED: If this reward is nested inside another (like ToggleReward),
                // its parent is responsible for updating it. Updating it here would cause
                // a double-update conflict, potentially firing deactivation rewards
                // prematurely.
                if (plr.isNested()) {
                    continue;
                }

                int totalLevel = getTotalSkillLevel(player, categoryId, skillId);

                // AUTO-DISABLE LOGIC: If Loot/Learned toggle skill has no level, force disable
                // it.
                var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
                if (config != null && config.toggle && config.lootMode != null && !config.lootMode.isEmpty()
                        && totalLevel <= 0) {
                    if (dataManager.isToggleActive(player, categoryId, skillId)) {
                        SkillLevelingMod.getInstance().getLogger()
                                .info("[Auto-Disable] Disabling toggle skill " + skillId + " because level is 0.");
                        dataManager.setToggleActive(player, categoryId, skillId, false);
                    }
                }

                // HYBRID REWARD LOGIC:
                // PerLevelRewardsReward instances (root rewards) ALWAYS receive the player's
                // actual level.
                // This keeps passive levels active even if someone "toggles off" a hybrid
                // skill.
                // The nested ToggleReward child (if any) will handle its own activation state.
                int rewardCount = totalLevel;

                // Trigger update with the calculated reward count.
                plr.update(new net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl(player, rewardCount, false));
            }
        }

        // Process ToggleReward instances
        for (var entry : toggleRewards.entrySet()) {
            Identifier categoryId = entry.getKey();
            for (var skillEntry : entry.getValue().entrySet()) {
                String skillId = skillEntry.getKey();
                ToggleReward tr = skillEntry.getValue();

                int totalLevel = getTotalSkillLevel(player, categoryId, skillId);

                // AUTO-DISABLE LOGIC: If Loot/Learned toggle skill has no level, force disable
                // it.
                var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
                if (config != null && config.toggle && config.lootMode != null && !config.lootMode.isEmpty()
                        && totalLevel <= 0) {
                    if (dataManager.isToggleActive(player, categoryId, skillId)) {
                        SkillLevelingMod.getInstance().getLogger()
                                .info("[Auto-Disable] Disabling toggle skill " + skillId + " (level=0)");
                        dataManager.setToggleActive(player, categoryId, skillId, false);
                    }
                }

                boolean isActive = dataManager.isToggleActive(player, categoryId, skillId);
                int rewardCount = isActive ? Math.max(1, totalLevel) : 0;

                // Trigger update with the calculated reward count.
                // action=false: ToggleReward internally forces action=true on genuine state
                // changes. Passing false here prevents commands from re-firing during routine
                // sync/refresh cycles.
                tr.update(new net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl(player, rewardCount, false));
            }
        }

        // SYNC FIX: Force attribute sync to client after all rewards have been updated.
        syncPlayerAttributes(player);

        // Ensure client gets the updated total levels immediately after a refresh.
        syncAllSkillsToPlayer(player);
    }

    /**
     * ATTRIBUTE SYNC: Sends an attribute update packet to the client.
     * This ensures that changes to attributes like max_health are reflected
     * in the player's UI immediately, not just on the server.
     */
    private void syncPlayerAttributes(ServerPlayerEntity player) {
        if (player == null || player.networkHandler == null) {
            return;
        }

        // AGGRESSIVE SYNC: Use getTracked() instead of getAttributesToSend()
        // This sends ALL attributes that are capable of being synced (health, speed,
        // etc.)
        // even if Minecraft doesn't think they are "dirty". This is necessary for
        // reliable realtime hearts.
        var attributesToSync = player.getAttributes().getTracked();
        if (!attributesToSync.isEmpty()) {
            player.networkHandler.sendPacket(
                    new net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket(
                            player.getId(), attributesToSync));
        }

        // HEALTH REFRESH: Force the client to recalculate hearts by triggering a health
        // update.
        // This ensures heart segments (red/empty) are redrawn correctly.
        player.setHealth(player.getHealth());
    }

    private boolean canAdvanceToLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        return canAdvanceToLevel(player, categoryId, skillId, level, false);
    }

    private boolean canAdvanceToLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level,
            boolean bypassPoints) {
        // Check if player meets UNLOCK requirements (top-level prerequisite_skills)
        boolean prereqMet = checkSkillPrerequisites(player.getUuid(), categoryId, skillId);
        if (!prereqMet) {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("[canAdvanceToLevel] FAILED: Prerequisites not met for " + skillId);
            return false;
        }

        // Check if player meets LEVEL requirements (required_skill_for_level)
        boolean levelPrereqMet = checkLevelPrerequisites(player.getUuid(), categoryId, skillId, level, false);
        if (!levelPrereqMet) {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("[canAdvanceToLevel] FAILED: Level prerequisites not met for " + skillId + " at level "
                            + level);
            return false;
        }

        // Check if player can afford the point cost
        if (!bypassPoints) {
            boolean canAfford = net.bluelotuscoding.skillleveling.points.SkillPointManager.canAffordLevel(player,
                    categoryId, skillId, level);
            if (!canAfford) {
                SkillLevelingMod.getInstance().getLogger()
                        .warn("[canAdvanceToLevel] FAILED: Cannot afford level " + level + " for " + skillId);
                return false;
            }
        }

        /*
         * SkillLevelingMod.getInstance().getLogger()
         * .debug("[canAdvanceToLevel] SUCCESS: All checks passed for " + skillId +
         * " at level " + level);
         */
        return true;
    }

    private void triggerLevelRewards(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        getPerLevelRewardsReward(categoryId, skillId).ifPresent(reward -> {
            int updateCount = level;
            boolean isAction = true;

            // NESTED TOGGLE FIX: If the reward is nested inside a ToggleReward,
            // the toggle state MUST be active for the rewards to trigger actions.
            if (reward.isNested()) {
                boolean isActive = dataManager.isToggleActive(player, categoryId, skillId);
                if (!isActive) {
                    updateCount = 0; // Force count to 0 to prevent level activation
                    isAction = false;
                }
            }

            reward.update(
                    new net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl(player, updateCount, isAction));
        });
    }

    /**
     * Refund one level of a skill for a player
     */
    /**
     * Check if refunding a skill to a lower level would break prerequisites for
     * other skills.
     * 
     * @return List of blocking skill names, or empty list if safe.
     */
    public List<String> checkPrerequisites(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int targetLevel) {
        var blockingSkills = new ArrayList<String>();
        try {
            // Check mod-bundled prerequisites
            for (var entry : perLevelRewardsRewards.entrySet()) {
                Identifier depCategoryId = entry.getKey();
                for (var depEntry : entry.getValue().entrySet()) {
                    String depSkillId = depEntry.getKey();
                    var depReward = depEntry.getValue();

                    if (depCategoryId.equals(categoryId) && depSkillId.equals(skillId))
                        continue;

                    for (var prereq : depReward.getRequiredSkills()) {
                        String prereqSkillId = prereq.getSkillId();
                        Identifier prereqCategory = prereq.getCategoryId() != null
                                ? new Identifier(prereq.getCategoryId())
                                : categoryId;

                        if (!prereqSkillId.equals(skillId))
                            continue;
                        if (!prereqCategory.equals(categoryId))
                            continue;

                        int requiredLevel = prereq.getLevel();
                        if (targetLevel < requiredLevel) {
                            int depCurrentLevel = getSkillLevel(player, depCategoryId, depSkillId);
                            if (depCurrentLevel > 0) {
                                blockingSkills.add(
                                        (depCategoryId.equals(categoryId) ? "" : (depCategoryId + ":")) + depSkillId);
                            }
                        }
                    }
                }
            }

            // Check config-defined prerequisites
            var leveledEntries = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.getAllEntries();
            for (var e : leveledEntries.entrySet()) {
                String depSkillId = e.getKey();
                var cfg = e.getValue();
                for (var req : cfg.requiredSkills) {
                    if (req.skillId.equals(skillId)) {
                        if (targetLevel < req.minLevel) {
                            Identifier depCategory = cfg.categoryId != null ? new Identifier(cfg.categoryId)
                                    : categoryId;
                            int depCurrentLevel = getSkillLevel(player, depCategory, depSkillId);
                            if (depCurrentLevel > 0) {
                                blockingSkills.add(depSkillId);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            SkillLevelingMod.getInstance().getLogger().error("Error checking prerequisites: " + e.getMessage());
        }
        return blockingSkills;
    }

    /**
     * Refund one level of a skill for a player
     */
    public boolean refundSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        // NORMALIZE: Ensure IDs from NBT/Command match registered registry IDs
        categoryId = normalizeCategoryId(categoryId);
        skillId = getCanonicalSkillId(categoryId, skillId);

        var category = SkillsAPI.getCategory(categoryId);
        if (category.isEmpty()) {
            return false;
        }

        var skill = category.get().getSkill(skillId);
        if (skill.isEmpty()) {
            return false;
        }

        Skill.State state = skill.get().getState(player);
        if (state != Skill.State.UNLOCKED && state != Skill.State.AFFORDABLE && state != Skill.State.AVAILABLE) {
            return false;
        }

        int currentBaseLevel = getBaseSkillLevel(player, categoryId, skillId);

        if (currentBaseLevel <= 0) {
            // Cannot refund below level 0
            return false;
        }

        // Atomic check for next level
        var blockers = checkPrerequisites(player, categoryId, skillId, currentBaseLevel - 1);
        if (!blockers.isEmpty()) {
            String joined = String.join(", ", blockers);
            player.sendMessage(net.minecraft.text.Text.translatable("skillleveling.refund.blocked_by_prereq", joined)
                    .formatted(net.minecraft.util.Formatting.RED), false);
            try {
                player.closeHandledScreen();
            } catch (Exception ignored) {
            }
            return false;
        }

        int newLevel = currentBaseLevel - 1;

        // Deactivate rewards for the level being refunded
        deactivateLevelRewards(player, categoryId, skillId, currentBaseLevel);

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

            // Clear the bit for the level being removed
            long bits = ext.addon$getPaidLevels(skillId);
            ext.addon$setPaidLevels(skillId, bits & ~(1L << currentBaseLevel));

            ext.addon$setSkillLevel(skillId, newLevel);
        }

        // REAL-TIME SYNC: Immediately notify client of level refund
        // Sync TOTAL level to client for UI
        int baseLevel = getBaseSkillLevel(player, categoryId, skillId);
        int totalLevel = getSkillLevel(player, categoryId, skillId);
        int maxLevel = getMaxLevel(categoryId, skillId);
        syncSkillLevelToClient(player, categoryId, skillId, baseLevel, totalLevel, maxLevel);

        // Ensure the core Skills mod updates its UI/state (points/spent calculation)
        try {
            net.puffish.skillsmod.SkillsMod.getInstance().updateAllCategories(player);
        } catch (Exception ignored) {
        }

        // Sync our category points to the client as well
        try {
            syncCategoryPoints(player, categoryId);
        } catch (Exception ignored) {
        }

        // removed forceFullCategorySync to prevent stuttering

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

    /**
     * TOGGLE SKILL: Logic for enabling/disabling skills
     */
    public boolean toggleSkill(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        ensureConfigurationsLoaded();

        SkillLevelingMod.getInstance().getLogger()
                .info("[toggleSkill] START - Player: " + player.getName().getString() +
                        ", Category: " + categoryId + ", Skill: " + skillId);

        final Identifier originalCatId = categoryId;
        categoryId = normalizeCategoryId(categoryId);

        if (!categoryId.equals(originalCatId)) {
            SkillLevelingMod.getInstance().getLogger()
                    .info("[toggleSkill] Normalized Category: " + originalCatId + " -> " + categoryId);
        }

        String canonicalId = getCanonicalSkillId(categoryId, skillId);
        if (!canonicalId.equals(skillId)) {
            SkillLevelingMod.getInstance().getLogger()
                    .info("[toggleSkill] Resolved Canonical ID: " + skillId + " -> " + canonicalId);
            skillId = canonicalId;
        }

        String definitionId = getDefinitionId(categoryId, skillId);
        var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(definitionId);
        if (config == null) {
            config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
        }

        if (config == null || !config.toggle) {
            return false;
        }

        int totalLevel = getTotalSkillLevel(player, categoryId, skillId);
        boolean currentlyActive = dataManager.isToggleActive(player, categoryId, skillId);
        boolean enabling = !currentlyActive;

        SkillLevelingMod.getInstance().getLogger().info(
                "[toggleSkill] Total Level: " + totalLevel + ", Currently Active: " + currentlyActive + ", Target: "
                        + (enabling ? "ENABLE" : "DISABLE"));

        if (enabling) {
            // Centralized prerequisites and level check
            if (!checkTogglePrerequisites(player, categoryId, skillId, true)) {
                return false;
            }

            // Cooldown check
            int remainingCooldown = getRemainingCooldown(player, skillId);
            if (remainingCooldown > 0) {
                setCooldown(player, skillId, remainingCooldown);
                return false;
            }

            // Enable
            SkillLevelingMod.getInstance().getLogger().info("[toggleSkill] ACTivating " + skillId);
            dataManager.setToggleActive(player, categoryId, skillId, true);
        } else {
            // Disable
            SkillLevelingMod.getInstance().getLogger().info("[toggleSkill] DEactivating " + skillId);
            dataManager.setToggleActive(player, categoryId, skillId, false);

            // Trigger cooldown
            if (config.cooldown > 0) {
                setCooldown(player, skillId, config.cooldown);
            }
        }

        // Apply changes immediately
        refreshAllRewards(player);
        return true;
    }

    public int getRemainingCooldown(ServerPlayerEntity player, String skillId) {
        var cooldowns = playerCooldowns.get(player.getUuid());
        if (cooldowns == null)
            return 0;
        Long expiry = cooldowns.get(skillId);
        if (expiry == null)
            return 0;

        long remainingMs = expiry - System.currentTimeMillis();
        if (remainingMs <= 0) {
            cooldowns.remove(skillId);
            return 0;
        }

        return (int) Math.ceil(remainingMs / 50.0); // Convert ms back to approximate ticks
    }

    private void setCooldown(ServerPlayerEntity player, String skillId, int ticks) {

        long expiry = System.currentTimeMillis() + (ticks * 50L);
        playerCooldowns.computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>()).put(skillId, expiry);

        // Robust category lookup for sync
        var category = findCategoryBySkillId(skillId);
        if (category != null) {
            net.bluelotuscoding.skillleveling.network.SkillLevelingNetwork.sendToggleCooldown(player, category, skillId,
                    ticks);
        } else {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("[setCooldown] Could not find category for skill " + skillId + " to sync cooldown!");
        }
    }

    private Identifier findCategoryBySkillId(String skillId) {
        // 1. Try finding in loaded per-level rewards (standard Pufferfish skills)
        for (var entry : perLevelRewardsRewards.entrySet()) {
            for (String registeredId : entry.getValue().keySet()) {
                if (isFuzzySkillMatch(registeredId, skillId)) {
                    return entry.getKey();
                }
            }
        }

        // 2. Fallback: Check config storage (Addon skills or not-yet-loaded skills)
        var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
        if (config != null && config.categoryId != null) {
            try {
                return new Identifier(config.categoryId);
            } catch (Exception e) {
                // Not a valid identifier
            }
        }

        return null;
    }

    public void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (var playerEntry : playerCooldowns.entrySet()) {
            var cooldowns = playerEntry.getValue();

            if (cooldowns.isEmpty())
                continue;

            // Safely update cooldowns
            cooldowns.entrySet().removeIf(entry -> {
                long remainingMs = entry.getValue() - now;

                // Log every ~2 seconds (40 ticks * 50ms = 2000ms)
                if (remainingMs > 0 && remainingMs <= 10000 && remainingMs % 2000 < 50) {
                }

                return remainingMs <= 0;
            });
        }
    }

    private void deactivateLevelRewards(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        getPerLevelRewardsReward(categoryId, skillId).ifPresent(reward -> {
            int updateCount = level - 1;
            boolean isAction = true;

            // NESTED TOGGLE FIX: If the reward is nested, and the toggle is inactive,
            // we update with count 0 to maintain consistency with the inactive state.
            if (reward.isNested()) {
                boolean isActive = dataManager.isToggleActive(player, categoryId, skillId);
                if (!isActive) {
                    updateCount = 0;
                    isAction = false;
                }
            }

            reward.update(
                    new net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl(player, updateCount, isAction));
        });
    }

    /**
     * Enhanced prerequisite checking for skills with prerequisite_skills
     * dependencies
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
     * Enhanced prerequisite checking for skills with prerequisite_skills
     * dependencies
     * 
     * @param playerId   The player UUID to check
     * @param categoryId The category of the skill
     * @param skillId    The skill to check prerequisites for
     * @return true if all prerequisites are met
     */
    public boolean checkSkillPrerequisites(UUID playerId, Identifier categoryId, String skillId) {
        // Check prerequisites from LeveledConfigStorage (defined at skill root -
        // prerequisite_skills)
        var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
        if (config != null && config.requiredSkills != null && !config.requiredSkills.isEmpty()) {

            for (var reqEntry : config.requiredSkills) {
                // Determine category for prerequisite (supports cross-category via categoryId
                // field)
                Identifier reqCategoryId = categoryId; // Default to same category
                if (reqEntry.categoryId != null && !reqEntry.categoryId.isEmpty()) {
                    // Use the specified category ID - path only lookup
                    reqCategoryId = findCategoryByPath(reqEntry.categoryId);
                    if (reqCategoryId == null) {
                        SkillLevelingMod.getInstance().getLogger().warn(
                                "[PREREQ_CHECK] Category not found for path: " + reqEntry.categoryId
                                        + ", falling back to same category");
                        reqCategoryId = categoryId; // Fallback to same category if not found
                    } else {
                    }
                }

                int currentLevel = getSkillLevelByUUID(playerId, reqCategoryId, reqEntry.skillId);

                if (currentLevel < reqEntry.minLevel) {
                    return false;
                }
            }
        }

        return true;
    }

    public void sendCloseScreenPacket(net.minecraft.server.network.ServerPlayerEntity player) {
        var handler = SkillLevelingMod.getInstance().getNetworkHandler();
        if (handler != null) {
            handler.sendToPlayer(new net.bluelotuscoding.skillleveling.network.CloseSkillScreenPacket(), player);
        }
    }

    /**
     * Check level-specific prerequisites (required_skill_for_level)
     * These gate specific levels of a skill behind other skill requirements.
     */
    public boolean checkLevelPrerequisites(UUID playerId, Identifier categoryId, String skillId, int targetLevel,
            boolean notify) {
        var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
        if (config == null || config.requiredSkillsForLevel == null || config.requiredSkillsForLevel.isEmpty()) {
            return true; // No level-specific prerequisites
        }

        // Check if there are prerequisites for this specific level
        var prereqsForLevel = config.requiredSkillsForLevel.get(targetLevel);
        if (prereqsForLevel == null || prereqsForLevel.isEmpty()) {
            return true; // No prerequisites for this level
        }

        List<String> missingPrereqs = new ArrayList<>();
        List<String> metPrereqs = new ArrayList<>();

        for (var reqEntry : prereqsForLevel) {
            // Determine category (supports cross-category via categoryId field)
            Identifier reqCategoryId = categoryId;
            if (reqEntry.categoryId != null && !reqEntry.categoryId.isEmpty()) {
                reqCategoryId = findCategoryByPath(reqEntry.categoryId);
                if (reqCategoryId == null) {
                    reqCategoryId = categoryId;
                }
            }

            int currentLevel = getSkillLevelByUUID(playerId, reqCategoryId, reqEntry.skillId);
            String categoryDisplay = reqEntry.categoryId != null ? " (" + reqEntry.categoryId + ")" : "";
            String prereqDesc = reqEntry.skillId + categoryDisplay + " Lv" + reqEntry.minLevel;

            if (currentLevel < reqEntry.minLevel) {
                missingPrereqs.add("§c✗ " + prereqDesc + " §7[Current: " + currentLevel + "]");

                // Debug logging - ONLY log if notifying player
                if (notify) {
                    int base = 0;
                    int bonus = 0;
                    var player = server != null ? server.getPlayerManager().getPlayer(playerId) : null;
                    if (player != null) {
                        base = dataManager.getSkillLevel(player, reqCategoryId, reqEntry.skillId);
                        bonus = calculateEquipmentBonus(player, reqCategoryId, reqEntry.skillId);
                    }

                }
            } else {
                metPrereqs.add("§a✓ " + prereqDesc);
            }
        }

        if (!missingPrereqs.isEmpty()) {
            if (notify && playerId != null && server != null) {
                var player = server.getPlayerManager().getPlayer(playerId);
                if (player != null) {
                    StringBuilder msg = new StringBuilder("§8[§6Skill Leveling§8] §cLevel Prerequisites not met:\n");
                    for (String missing : missingPrereqs) {
                        msg.append("§7 - ").append(missing).append("\n");
                    }
                    if (!metPrereqs.isEmpty()) {
                        msg.append("§Met:\n");
                        for (String met : metPrereqs) {
                            msg.append("§8 - ").append(met).append("\n");
                        }
                    }
                    player.sendMessage(net.minecraft.text.Text.literal(msg.toString().trim()), false);
                    sendCloseScreenPacket(player);
                }
            }
            return false;
        }

        return true;
    }

    /**
     * TOGGLE GUARD: Checks all prerequisites before allowing a skill to be toggled.
     * Provides detailed feedback and closes screen on failure.
     */
    public boolean checkTogglePrerequisites(ServerPlayerEntity player, Identifier categoryId, String skillId,
            boolean notify) {
        String definitionId = getDefinitionId(categoryId, skillId);
        var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(definitionId);
        if (config == null) {
            config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
        }

        List<String> missingPrereqs = new ArrayList<>();
        UUID playerId = player.getUuid();

        // 1. Root skill prerequisites
        if (config != null && config.requiredSkills != null) {
            for (var reqEntry : config.requiredSkills) {
                Identifier reqCategoryId = categoryId;
                if (reqEntry.categoryId != null && !reqEntry.categoryId.isEmpty()) {
                    reqCategoryId = findCategoryByPath(reqEntry.categoryId);
                    if (reqCategoryId == null)
                        reqCategoryId = categoryId;
                }

                int currentLevel = getSkillLevelByUUID(playerId, reqCategoryId, reqEntry.skillId);
                if (currentLevel < reqEntry.minLevel) {
                    String categoryDisplay = reqEntry.categoryId != null ? " (" + reqEntry.categoryId + ")" : "";
                    missingPrereqs.add("§c✗ " + reqEntry.skillId + categoryDisplay + " Lv" + reqEntry.minLevel
                            + " §7[Current: " + currentLevel + "]");
                }
            }
        }

        // 2. Minimum level check
        int totalLevel = getTotalSkillLevel(player, categoryId, skillId);
        int currentMaxLevel = getMaxLevel(categoryId, skillId);

        // NEW LOGIC FOR BASIC TOGGLES:
        // If max_skill_level is 0 or 1, it's a pure/basic toggle - allow at any level.
        if (currentMaxLevel <= 1) {
            // Pure toggle (0) or basic toggle (1) - always allowed
        } else if (totalLevel < 1) {
            // Hybrid skill (levels > 1) - requires at least level 1
            missingPrereqs.add("§c✗ " + skillId + " Lv1 §7[Current: " + totalLevel + "]");
        }

        // 3. Minimum toggle level requirement from rewards
        int minToggleLevel = -1;
        var perLevelMap = perLevelRewardsRewards.get(categoryId);
        if (perLevelMap != null) {
            var perLevelReward = perLevelMap.get(skillId);
            if (perLevelReward != null) {
                for (var entry : perLevelReward.getLevelRewards().entrySet()) {
                    for (var rewardCfg : entry.getValue()) {
                        if (rewardCfg.type().toString().equals("puffish_skill_leveling:toggle")) {
                            if (minToggleLevel == -1 || entry.getKey() < minToggleLevel) {
                                minToggleLevel = entry.getKey();
                            }
                        }
                    }
                }
            }
        }

        if (minToggleLevel != -1 && totalLevel < minToggleLevel) {
            missingPrereqs.add("§c✗ " + skillId + " Lv" + minToggleLevel + " §7[Current: " + totalLevel + "]");
        }

        if (!missingPrereqs.isEmpty()) {
            if (notify) {
                net.minecraft.text.MutableText msg = net.minecraft.text.Text
                        .translatable("skillleveling.toggle.failure_header").append("\n");
                for (int i = 0; i < missingPrereqs.size(); i++) {
                    msg.append(net.minecraft.text.Text.literal("§7 - " + missingPrereqs.get(i)));
                    if (i < missingPrereqs.size() - 1) {
                        msg.append(net.minecraft.text.Text.literal("\n"));
                    }
                }
                player.sendMessage(msg, false);
                sendCloseScreenPacket(player);
            }
            return false;
        }

        return true;
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
                player.sendMessage(net.minecraft.text.Text.translatable(
                        "skillleveling.tome.max_level")
                        .formatted(net.minecraft.util.Formatting.RED), false);
                return false;
            }

            // LOG XP FOR DEBUGGING

            // Grant the experience
            experience.addTotal(player, xpNeeded);

            // NOTE: Do NOT call forceFullCategorySync here!
            // experience.addTotal already triggers the internal Pufferfish Skills sync,
            // and calling updateAllCategories again would cause double processing.

            // Capture and log the post-state
            int newLevel = experience.getLevel(player);
            int newTotalXp = experience.getTotal(player);
            int expectedNextLevelXp = experience.getRequiredTotal(newLevel + 1);

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
     * Recursively register rewards to ensure nested instances are initialized.
     */
    private void registerRewardRecursive(Identifier categoryId, String skillId,
            net.puffish.skillsmod.api.reward.Reward reward) {
        if (reward instanceof net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward plr) {
            if (plr.getSkillId() != null && !plr.getSkillId().equals(skillId)) {
                // Skip if it explicitly belongs to another skill (uncommon for nested)
            } else {
                plr.setCachedCategoryId(categoryId);
                // Only register as the primary PLR if none exists yet for this skill
                if (!perLevelRewardsRewards.getOrDefault(categoryId, Map.of()).containsKey(skillId)) {
                    registerPerLevelRewardsReward(categoryId, skillId, plr);
                }

                // Drill down
                for (var levelList : plr.getLevelRewards().values()) {
                    for (var cfg : levelList) {
                        registerRewardRecursive(categoryId, skillId, cfg.instance());
                    }
                }
                for (var disableList : plr.getOnDisableLevelRewards().values()) {
                    for (var cfg : disableList) {
                        registerRewardRecursive(categoryId, skillId, cfg.instance());
                    }
                }
            }
        } else if (reward instanceof net.bluelotuscoding.skillleveling.rewards.ToggleReward tr) {
            tr.setCachedCategoryId(categoryId);
            tr.setCachedSkillId(skillId);
            if (!toggleRewards.getOrDefault(categoryId, Map.of()).containsKey(skillId)) {
                toggleRewards.computeIfAbsent(categoryId, k -> new HashMap<>()).put(skillId, tr);
            }

            // Drill down
            for (var cfg : tr.getEnableRewards()) {
                if (cfg.instance() instanceof net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward nestedPlr) {
                    nestedPlr.setNested(true);
                }
                registerRewardRecursive(categoryId, skillId, cfg.instance());
            }
            for (var cfg : tr.getDisableRewards()) {
                if (cfg.instance() instanceof net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward nestedPlr) {
                    nestedPlr.setNested(true);
                }
                registerRewardRecursive(categoryId, skillId, cfg.instance());
            }
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

        boolean success = refundSkillLevel(player, categoryId, skillId);
        if (success) {
            syncCategoryPoints(player, categoryId);
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

        // ATOMIC CHECK: Verify FULL reset to 0 is possible manually
        var blockers = checkPrerequisites(player, categoryId, skillId, 0);
        if (!blockers.isEmpty()) {
            String joined = String.join(", ", blockers);
            player.sendMessage(net.minecraft.text.Text.translatable("skillleveling.refund.blocked_by_prereq", joined)
                    .formatted(net.minecraft.util.Formatting.RED), false);
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
                // DO NOT add points here! The point recalculation happens automatically
                // when we reduce the skill level in dataManager.
                // categoryOpt.get().addPoints(player,
                // net.puffish.skillsmod.util.PointSources.COMMANDS, pointsRefunded);

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
                // Normalize ID to handle namespace changes (legacy support)
                Identifier normalizedId = normalizeCategoryId(categoryId);
                var categoryData = playerExt.addon$getCategoryData(normalizedId);
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
     * Resolves the current registry category ID from a potentially legacy ID
     * (path-based matching).
     */
    public Identifier normalizeCategoryId(Identifier categoryId) {
        if (categoryId == null)
            return null;

        // Direct hit
        if (SkillsAPI.getCategory(categoryId).isPresent()) {
            return categoryId;
        }

        // Path search fallback for namespace migrations (CASE-INSENSITIVE)
        return SkillsAPI.streamCategories()
                .filter(cat -> cat.getId().getPath().equalsIgnoreCase(categoryId.getPath()))
                .map(net.puffish.skillsmod.api.Category::getId)
                .findFirst()
                .orElse(categoryId);
    }

    /**
     * Find a category by its path (without namespace) or full ID string.
     */
    public Identifier findCategoryByPath(String categoryPath) {
        if (categoryPath == null || categoryPath.isEmpty()) {
            return null;
        }

        // Support full ID as well
        if (categoryPath.contains(":")) {
            try {
                Identifier id = new Identifier(categoryPath);
                return normalizeCategoryId(id);
            } catch (Exception e) {
                // Not a valid identifier, fall through to path matching
            }
        }

        // CASE-INSENSITIVE path matching
        return SkillsAPI.streamCategories()
                .filter(cat -> cat.getId().getPath().equalsIgnoreCase(categoryPath))
                .map(net.puffish.skillsmod.api.Category::getId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Process a tome action from client request.
     * This is the central dispatcher for tome functionality.
     */
    public void processTomeAction(ServerPlayerEntity player, Identifier categoryId, String skillId,
            net.bluelotuscoding.skillleveling.item.TomeItem.TomeType tomeType) {
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
            if (categoryOpt.isEmpty()) {
                return;
            }

            var category = categoryOpt.get();
            category.streamSkills().forEach(skill -> {
                String skillId = skill.getId();
                // CRITICAL FIX: Use getBaseSkillLevel for baseLevel, getSkillLevel for
                // totalLevel
                int baseLevel = getBaseSkillLevel(player, categoryId, skillId);
                int totalLevel = getSkillLevel(player, categoryId, skillId);
                int maxLevel = getMaxLevel(categoryId, skillId);

                // Sync ALL skills, even those at level 0, to ensure bonuses clear in UI
                syncSkillLevelToClient(player, categoryId, skillId, baseLevel, totalLevel, maxLevel);

            });
        } catch (Exception e) {
            SkillLevelingMod.getInstance().getLogger()
                    .error("Failed to sync all skills in category: " + e.getMessage());
        }
    }
}