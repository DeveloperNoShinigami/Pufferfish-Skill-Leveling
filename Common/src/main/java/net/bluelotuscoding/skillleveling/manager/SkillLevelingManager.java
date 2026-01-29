package net.bluelotuscoding.skillleveling.manager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.Skill;
import net.bluelotuscoding.skillleveling.data.SkillLevelingDataManager;
import net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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
    private MinecraftServer server;
    private boolean configurationsLoaded = false;
    private boolean networkHandlerNullWarned = false;
    // Track which definitionIds have been sent to which players to avoid redundant
    // sends

    public SkillLevelingManager() {
        this.dataManager = new SkillLevelingDataManager();
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

    /**
     * CLIENT SYNC: Extended version with definition ID for description mapping.
     */
    public void syncSkillLevelToClient(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int baseLevel, int totalLevel, int maxLevel, String definitionId) {
        syncSkillLevelToClient(player, categoryId, skillId, baseLevel, totalLevel, maxLevel,
                getPointsForLevel(categoryId, skillId, 1), definitionId);
    }

    public void syncSkillLevelToClient(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int baseLevel, int totalLevel, int maxLevel, int pointsPerLevel, String definitionId) {
        try {
            var networkHandler = SkillLevelingMod.getInstance().getNetworkHandler();
            if (networkHandler != null) {
                networkHandler.sendToPlayer(
                        new net.bluelotuscoding.skillleveling.network.SyncSkillLevelPacket(categoryId, skillId,
                                baseLevel, totalLevel, maxLevel, pointsPerLevel, definitionId),
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
        boolean imbueOnly = false;
        var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(definitionId);
        if (config != null && config.lootMode != null) {
            imbueOnly = config.lootMode.equals("imbue_only");
        }

        try {
            var networkHandler = SkillLevelingMod.getInstance().getNetworkHandler();
            if (networkHandler != null) {
                try {
                    SkillLevelingMod.getInstance().getLogger()
                            .info("[DEBUG] Preparing to send descriptions to " + player.getName().getString() + " -> "
                                    + definitionId + " (levels=" + levelDescriptions.size() + ", extras="
                                    + levelExtraDescriptions.size() + ")");
                } catch (Exception ignored) {
                }
                try {
                    SkillLevelingMod.getInstance().getLogger().info("[ADDON] Sending SyncSkillDescriptionsPacket to "
                            + player.getName().getString() + " -> " + definitionId);
                } catch (Exception ignored) {
                }
                networkHandler.sendToPlayer(
                        new net.bluelotuscoding.skillleveling.network.SyncSkillDescriptionsPacket(
                                definitionId, levelDescriptions, levelExtraDescriptions, mergeDescription, maxLevel,
                                imbueOnly),
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
        int syncedSkills = 0;
        try {
            SkillLevelingMod.getInstance().getLogger()
                    .info("[ADDON] Starting full skill sync for player " + player.getName().getString()
                            + " (registered skills="
                            + perLevelRewardsRewards.values().stream().mapToInt(m -> m.size()).sum() + ")");
        } catch (Exception ignored) {
        }

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

                    // Always attempt to send descriptions on join so clients receive mapping
                    // (some clients relied on this previously; sending empty maps is safe)
                    Map<Integer, String> levelDescsSafe = levelDescs != null ? levelDescs
                            : java.util.Collections.<Integer, String>emptyMap();
                    Map<Integer, String> extraDescsSafe = extraDescs != null ? extraDescs
                            : java.util.Collections.<Integer, String>emptyMap();
                    // Try to resolve definitionId via reflection, fallback to skillId
                    String definitionId = skillId;
                    try {
                        try {
                            var skillsMod = net.puffish.skillsmod.SkillsMod.getInstance();
                            var getCategoryMethod = net.puffish.skillsmod.SkillsMod.class.getDeclaredMethod(
                                    "getCategory",
                                    Identifier.class);
                            getCategoryMethod.setAccessible(true);
                            var categoryConfigOpt = (java.util.Optional<?>) getCategoryMethod.invoke(skillsMod,
                                    categoryId);

                            if (categoryConfigOpt.isPresent()) {
                                var categoryConfig = categoryConfigOpt.get();
                                var skillsMethod = categoryConfig.getClass().getMethod("skills");
                                var skillsConfig = skillsMethod.invoke(categoryConfig);

                                var getByIdMethod = skillsConfig.getClass().getMethod("getById", String.class);
                                var skillConfigOpt = (java.util.Optional<?>) getByIdMethod.invoke(skillsConfig,
                                        skillId);

                                if (skillConfigOpt.isPresent()) {
                                    var skillConfig = skillConfigOpt.get();
                                    definitionId = (String) skillConfig.getClass().getMethod("definitionId")
                                            .invoke(skillConfig);
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    } catch (Exception ignored) {
                    }

                    try {
                        SkillLevelingMod.getInstance().getLogger()
                                .info("[ADDON] Preparing descriptions for " + categoryId + "/" + skillId + " -> def="
                                        + definitionId + " (levels=" + levelDescsSafe.size() + ", extras="
                                        + extraDescsSafe.size() + ")");
                    } catch (Exception ignored) {
                    }
                    // Send descriptions to client for both definitionId and skillId fallback
                    syncDescriptionsToClient(player, definitionId, levelDescsSafe, extraDescsSafe, merge,
                            plr.getMaxLevel());
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

                    // Resolve definitionId for the level packet too
                    String definitionId = skillId;
                    try {
                        var method = skill.getClass().getMethod("definitionId");
                        definitionId = (String) method.invoke(skill);
                    } catch (Exception ignored) {
                    }

                    syncSkillLevelToClient(player, categoryId, skillId, baseLevel, totalLevel, maxLevel,
                            definitionId);
                    syncedSkills++;
                } catch (Exception e) {
                    SkillLevelingMod.getInstance().getLogger()
                            .error("Failed to sync skill " + skillId + ": " + e.getMessage());
                }
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
        this.configurationsLoaded = false;
        this.perLevelRewardsRewards.clear();
        ensureConfigurationsLoaded();
    }

    public void onServerStopping(MinecraftServer server) {
        dataManager.saveAll();
    }

    public void onPlayerJoin(ServerPlayerEntity player) {
        // Ensure configurations are loaded before any skill lookups
        ensureConfigurationsLoaded();

        // Load player skill level data from disk
        dataManager.loadPlayerData(player);

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

        // Sync all skill levels and descriptions to client for UI display
        syncAllSkillsToPlayer(player);

        // REFRESH REWARDS: Ensure imbued bonuses apply their attribute modifiers etc.
        refreshAllRewards(player);
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
        var catMap = perLevelRewardsRewards.get(categoryId);
        if (catMap != null && catMap.containsKey(skillId)) {
            return Optional.of(catMap.get(skillId));
        }

        // Lazy load and try again
        ensureConfigurationsLoaded();
        catMap = perLevelRewardsRewards.get(categoryId);
        if (catMap != null) {
            return Optional.ofNullable(catMap.get(skillId));
        }
        return Optional.empty();
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
        int bonusLevel = calculateEquipmentBonus(player, categoryId, skillId);
        return baseLevel + bonusLevel;
    }

    /**
     * Get only the base (purchased/persisted) skill level.
     */
    public int getBaseSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        // Try to get from Mixin first as it's the most "live" during a session
        var ext = getCategoryDataExtension(player, categoryId);
        if (ext != null) {
            return ext.addon$getSkillLevel(skillId);
        }

        // FALLBACK: always check persisted storage
        return dataManager.getSkillLevel(player, categoryId, skillId);
    }

    /**
     * @deprecated Use {@link #getTotalSkillLevel} or {@link #getBaseSkillLevel}
     */
    @Deprecated
    public int getSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return getTotalSkillLevel(player, categoryId, skillId);
    }

    /**
     * Calculate additional skill levels granted by equipped items.
     * Scans main hand, off hand, and armor slots.
     */
    public int calculateEquipmentBonus(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        int bonus = 0;

        // Scan all equipment slots
        for (var slot : net.minecraft.entity.EquipmentSlot.values()) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) {
                continue;
            }

            NbtCompound nbt = stack.getNbt();
            if (nbt != null && nbt.contains("SkillLevelingImbued", net.minecraft.nbt.NbtElement.COMPOUND_TYPE)) {
                NbtCompound imbueNbt = nbt.getCompound("SkillLevelingImbued");
                String imbueCategory = imbueNbt.getString("CategoryId");
                String imbueSkill = imbueNbt.getString("SkillId");

                if (imbueCategory != null && !imbueCategory.isEmpty()) {
                    String normalizedImbueCategory = imbueCategory.contains(":") ? imbueCategory
                            : "skillleveling_template:" + imbueCategory;
                    String normalizedTargetCategory = categoryId.toString(); // Identifier.toString() always includes
                                                                             // namespace

                    if (normalizedTargetCategory.equals(normalizedImbueCategory) && skillId.equals(imbueSkill)) {
                        int level = imbueNbt.contains("Level") ? imbueNbt.getInt("Level") : 1;
                        bonus += level;
                    }
                }
            }
        }

        return bonus;
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
    public void ensureConfigurationsLoaded() {
        if (configurationsLoaded) {
            return;
        }

        SkillLevelingMod.getInstance().getLogger().info("[CONFIG LOAD] Starting configuration discovery...");

        try {
            var mod = net.puffish.skillsmod.SkillsMod.getInstance();
            if (mod == null) {
                SkillLevelingMod.getInstance().getLogger().error("[CONFIG LOAD] SkillsMod instance is null!");
                return;
            }

            int categoryCount = 0;
            int skillCount = 0;

            for (var category : net.puffish.skillsmod.api.SkillsAPI.streamCategories().toList()) {
                try {
                    Identifier categoryId = category.getId();
                    SkillLevelingMod.getInstance().getLogger().info("[CONFIG LOAD] Processing category: " + categoryId);
                    categoryCount++;

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
                                            var rewardInstance = rewardObj.getClass().getMethod("instance")
                                                    .invoke(rewardObj);
                                            if (rewardInstance instanceof net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward plr) {
                                                registerPerLevelRewardsReward(categoryId, skillId, plr);
                                                skillCount++;
                                                SkillLevelingMod.getInstance().getLogger()
                                                        .debug("[CONFIG LOAD] Registered PLR for: " + categoryId + "/"
                                                                + skillId + " (maxLevel=" + plr.getMaxLevel() + ")");

                                                // (reverted) do not send descriptions here; keep join-time sync logic
                                                // simple
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

            SkillLevelingMod.getInstance().getLogger().info("[CONFIG LOAD] Discovery complete: " + categoryCount
                    + " categories, " + skillCount + " skills registered");
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

        SkillLevelingMod.getInstance().getLogger().info("[TOME DEBUG] Advancing " + skillId + ": currentBase="
                + currentBaseLevel + ", newBase=" + newBaseLevel + ", max=" + maxLevel + ", bypass="
                + bypassPrerequisites);

        if (newBaseLevel > maxLevel) {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("Cannot advance " + skillId + ": already at max level " + maxLevel);
            return false;
        }

        // Check if advancement is allowed (skip if bypassing prerequisites)
        if (!bypassPrerequisites && !canAdvanceToLevel(player, categoryId, skillId, newBaseLevel)) {
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

        int currentBaseLevel = getBaseSkillLevel(player, categoryId, skillId);

        // If we're setting a level higher than current, check requirements
        if (level > currentBaseLevel && !canAdvanceToLevel(player, categoryId, skillId, level)) {
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

        // REAL-TIME SYNC: Immediately notify client of level change
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

                int totalLevel = getTotalSkillLevel(player, categoryId, skillId);
                SkillLevelingMod.getInstance().getLogger()
                        .debug("[REWARD] Refreshing " + skillId + " to level " + totalLevel);
                // Trigger update with the new total level.
                // Action is true here (even though it's a refresh) to force Pufferfish
                // attribute rewards to re-evaluate and apply/revert attribute modifiers.
                plr.update(new net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl(player, totalLevel, true));
            }
        }

        // SYNC FIX: Force attribute sync to client after all rewards have been updated.
        // This ensures that health, armor, and other visual attributes update in
        // real-time.
        syncPlayerAttributes(player);
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
        // Check if player meets level requirements (using our custom prerequisite
        // check)
        if (!checkSkillPrerequisites(player.getUuid(), categoryId, skillId)) {
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
                    "[TOME DEBUG] Before: Level=" + currentLevel
                            + ", TotalXP=" + totalXpNow
                            + ", XPNeededForNext=" + xpNeeded);

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
                    "[TOME DEBUG] After: Level=" + newLevel
                            + ", TotalXP=" + newTotalXp
                            + ", ExpectedLevel=" + (currentLevel + 1)
                            + ", NextLevelTargetXP=" + expectedNextLevelXp);

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

        boolean success = refundSkillLevel(player, categoryId, skillId);
        if (success) {
            syncCategoryPoints(player, categoryId);
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
            if (categoryOpt.isEmpty()) {
                return;
            }

            var category = categoryOpt.get();
            category.streamSkills().forEach(skill -> {
                String skillId = skill.getId();
                // CRITICAL FIX: Use getSkillLevel instead of dataManager.getSkillLevel
                int level = getSkillLevel(player, categoryId, skillId);
                int maxLevel = getMaxLevel(categoryId, skillId);

                // Sync ALL skills, even those at level 0, to ensure bonuses clear in UI
                int totalLevel = getSkillLevel(player, categoryId, skillId);
                syncSkillLevelToClient(player, categoryId, skillId, level, totalLevel, maxLevel);
            });
        } catch (Exception e) {
            SkillLevelingMod.getInstance().getLogger()
                    .error("Failed to sync all skills in category: " + e.getMessage());
        }
    }
}