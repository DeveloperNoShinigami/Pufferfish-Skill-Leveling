package net.bluelotuscoding.skillleveling;

import net.bluelotuscoding.skillleveling.manager.SkillLevelingManager;
import net.bluelotuscoding.skillleveling.integration.SkillsModEventHandler;
import net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward;
import net.bluelotuscoding.skillleveling.rewards.EffectReward;
import net.bluelotuscoding.skillleveling.rewards.ToggleReward;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.bluelotuscoding.skillleveling.bridge.BridgeConfigManager;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * ADDON MAIN CLASS: Integrates with Pufferfish Skills to provide per-level
 * skill rewards
 * 
 * COMPLETE REFACTOR FROM FORK VERSION:
 * 
 * This is no longer a modification of the Skills mod - it's a true addon that
 * works
 * alongside the official Skills mod. The key architectural changes:
 * 
 * 1. INTEGRATION NOT REPLACEMENT: We hook into Skills mod events rather than
 * replace functionality
 * 2. SEPARATE DATA STORAGE: Our level data is stored independently from Skills
 * mod data
 * 3. API-DRIVEN VALIDATION: All operations validate against Skills API instead
 * of internal state
 * 4. EVENT-DRIVEN ARCHITECTURE: React to Skills mod events to maintain level
 * data consistency
 * 
 * COMPATIBILITY STRATEGY:
 * - Skills mod handles skill unlock/lock (levels 0 ↔ 1)
 * - Our addon handles level progression (levels 1 → 2 → 3 → N)
 * - Combined system provides seamless multi-level skill progression
 * 
 * DESIGN PHILOSOPHY:
 * - "Do one thing well": Focus purely on level progression, let Skills mod
 * handle everything else
 * - "Fail gracefully": If Skills mod isn't available, addon should disable
 * cleanly
 * - "Data independence": Our data should never corrupt or conflict with Skills
 * mod data
 */
public class SkillLevelingMod {
    public static final String MOD_ID = "puffish_skill_leveling";

    // ADDON CORE COMPONENTS: Independent systems that work alongside Skills mod
    private static SkillLevelingMod instance;
    private final SkillLevelingManager skillLevelingManager;
    private final SkillsModEventHandler eventHandler;
    private final AddonLogger logger;
    private final net.bluelotuscoding.skillleveling.data.SkillMasterTradeLoader tradeLoader;
    private final net.bluelotuscoding.skillleveling.data.SkillMasterReputationLoader reputationLoader;
    private final net.bluelotuscoding.skillleveling.data.ExpTomeConfigLoader expTomeConfigLoader;
    private final net.bluelotuscoding.skillleveling.bridge.config.EpicClassDataLoader epicClassDataLoader;
    private final net.bluelotuscoding.skillleveling.bridge.config.JobMasterDataLoader jobMasterDataLoader;
    private final net.bluelotuscoding.skillleveling.bridge.config.EpicAttributeDataLoader epicAttributeDataLoader;
    private final net.bluelotuscoding.skillleveling.bridge.config.BridgeDataLoader bridgeDataLoader;
    private final net.bluelotuscoding.skillleveling.bridge.config.ItemRequirementsManager itemRequirementsManager;

    private final net.bluelotuscoding.skillleveling.loot.LootImbueManager lootImbueManager;
    private final net.bluelotuscoding.skillleveling.loot.UniversalLootHandler universalLootHandler;
    private net.bluelotuscoding.skillleveling.network.NetworkHandler networkHandler;
    private net.bluelotuscoding.skillleveling.integration.EquipmentScanner equipmentScanner = entity -> java.util.Collections
            .emptyList();
    private net.bluelotuscoding.skillleveling.util.Platform platform;

    private SkillLevelingMod() {
        // INITIALIZE ADDON SYSTEMS: Set up our independent infrastructure
        this.skillLevelingManager = new SkillLevelingManager();
        this.eventHandler = new SkillsModEventHandler(skillLevelingManager);
        this.logger = new AddonLogger();
        this.tradeLoader = new net.bluelotuscoding.skillleveling.data.SkillMasterTradeLoader();
        this.reputationLoader = new net.bluelotuscoding.skillleveling.data.SkillMasterReputationLoader();
        this.expTomeConfigLoader = new net.bluelotuscoding.skillleveling.data.ExpTomeConfigLoader();
        this.epicClassDataLoader = new net.bluelotuscoding.skillleveling.bridge.config.EpicClassDataLoader();
        this.jobMasterDataLoader = new net.bluelotuscoding.skillleveling.bridge.config.JobMasterDataLoader();
        this.epicAttributeDataLoader = new net.bluelotuscoding.skillleveling.bridge.config.EpicAttributeDataLoader();
        this.bridgeDataLoader = new net.bluelotuscoding.skillleveling.bridge.config.BridgeDataLoader();
        this.itemRequirementsManager = new net.bluelotuscoding.skillleveling.bridge.config.ItemRequirementsManager();

        this.lootImbueManager = new net.bluelotuscoding.skillleveling.loot.LootImbueManager();
        this.universalLootHandler = new net.bluelotuscoding.skillleveling.loot.UniversalLootHandler();
    }

    /**
     * ADDON INITIALIZATION: Sets up integration with the Skills mod
     * 
     * PHASE 1 FOUNDATION COMPLETE: This method now initializes our refactored
     * addon architecture that works alongside the official Skills mod rather
     * than replacing its functionality.
     * 
     * INITIALIZATION SEQUENCE:
     * 1. Create addon instance with independent systems
     * 2. Register our PerLevelRewardsReward type with Skills mod
     * 3. Hook into Skills mod events for skill unlock/lock detection
     * 4. Set up server lifecycle listeners for data management
     */
    public static void preInit() {
        if (instance == null) {
            instance = new SkillLevelingMod();
        }
    }

    public static void init(java.io.File configDir) {
        preInit();

        // INITIALIZE CONFIGURATION (placeholder — config file system is planned for a
        // future update)
        net.bluelotuscoding.skillleveling.config.SkillLevelingConfig.load(configDir);
        BridgeConfigManager.load(configDir);
        EpicClassBridge.loadConfig(BridgeConfigManager.getConfig());
        net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager.load(configDir);

        instance.logger.info("Initializing Pufferfish Skill Leveling addon v2 (Logging Balanced)...");

        // REGISTER REWARD TYPE: Add our per-level rewards to Skills mod's reward system
        PerLevelRewardsReward.register();
        EffectReward.register();
        ToggleReward.register();
        instance.logger.info("Registered custom reward types (Per-Level, Effect, Toggle)");

        // SKILLS MOD EVENT INTEGRATION: Hook into unlock/lock events
        instance.eventHandler.registerEventHandlers();
        instance.logger.info("Registered Skills mod event handlers");

        // SERVER LIFECYCLE INTEGRATION: Lifecycle events are handled by the
        // platform-specific
        // main classes (e.g., ForgeMain) which call into the manager's server hooks.
        instance.logger.info("Server lifecycle events are managed by platform runners");

        instance.logger.info("Addon initialization complete - ready for skill level progression!");
    }

    // ===========================================
    // ADDON API ACCESS (PUBLIC INTERFACE)
    // ===========================================

    public static SkillLevelingMod getInstance() {
        return instance;
    }

    public void setNetworkHandler(net.bluelotuscoding.skillleveling.network.NetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
        // Reset manager warning state so a previous null warning doesn't persist
        try {
            if (this.skillLevelingManager != null) {
                java.lang.reflect.Field f = this.skillLevelingManager.getClass()
                        .getDeclaredField("networkHandlerNullWarned");
                f.setAccessible(true);
                f.setBoolean(this.skillLevelingManager, false);
            }
        } catch (Exception ignored) {
        }
    }

    public net.bluelotuscoding.skillleveling.network.NetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    public SkillLevelingManager getSkillLevelingManager() {
        return skillLevelingManager;
    }

    public AddonLogger getLogger() {
        return logger;
    }

    public net.bluelotuscoding.skillleveling.data.SkillMasterTradeLoader getTradeLoader() {
        return tradeLoader;
    }

    public net.bluelotuscoding.skillleveling.data.SkillMasterReputationLoader getReputationLoader() {
        return reputationLoader;
    }

    public net.bluelotuscoding.skillleveling.data.ExpTomeConfigLoader getExpTomeConfigLoader() {
        return expTomeConfigLoader;
    }

    public net.bluelotuscoding.skillleveling.bridge.config.EpicClassDataLoader getEpicClassDataLoader() {
        return epicClassDataLoader;
    }

    public net.bluelotuscoding.skillleveling.bridge.config.JobMasterDataLoader getJobMasterDataLoader() {
        return jobMasterDataLoader;
    }

    public net.bluelotuscoding.skillleveling.bridge.config.EpicAttributeDataLoader getEpicAttributeDataLoader() {
        return epicAttributeDataLoader;
    }

    public net.bluelotuscoding.skillleveling.bridge.config.BridgeDataLoader getBridgeDataLoader() {
        return bridgeDataLoader;
    }

    public net.bluelotuscoding.skillleveling.bridge.config.ItemRequirementsManager getItemRequirementsManager() {
        return itemRequirementsManager;
    }

    public net.bluelotuscoding.skillleveling.loot.LootImbueManager getLootImbueManager() {
        return lootImbueManager;
    }

    public net.bluelotuscoding.skillleveling.loot.UniversalLootHandler getUniversalLootHandler() {
        return universalLootHandler;
    }

    public void setEquipmentScanner(net.bluelotuscoding.skillleveling.integration.EquipmentScanner equipmentScanner) {
        this.equipmentScanner = equipmentScanner;
    }

    public net.bluelotuscoding.skillleveling.integration.EquipmentScanner getEquipmentScanner() {
        return equipmentScanner;
    }

    public void setPlatform(net.bluelotuscoding.skillleveling.util.Platform platform) {
        this.platform = platform;
    }

    public net.bluelotuscoding.skillleveling.util.Platform getPlatform() {
        return platform;
    }

    public static Identifier createIdentifier(String path) {
        return new Identifier(MOD_ID, path);
    }

    public void syncBridgeContent(ServerPlayerEntity player) {
        if (this.networkHandler != null) {
            this.networkHandler.sendToPlayer(
                    new net.bluelotuscoding.skillleveling.network.SyncBridgeContentPacket(
                            net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager.getClasses(),
                            net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager
                                    .getAttributePagesMap(),
                            net.bluelotuscoding.skillleveling.bridge.BridgeConfigManager.getConfig()),
                    player);
        }
    }

    // ================================================
    // SKILL LEVEL API (DELEGATE TO MANAGER)
    // ================================================

    /**
     * LEVEL QUERY: Check if a player has reached a specific skill level
     * 
     * ADDON BEHAVIOR: Returns true only if the Skills mod reports the skill
     * as unlocked AND our level data shows the player has reached that level.
     */
    public boolean hasSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        return skillLevelingManager.hasSkillLevel(player, categoryId, skillId, level);
    }

    /**
     * Get the TOTAL skill level (Base + Equipment Bonus)
     */
    public int getTotalSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return skillLevelingManager.getTotalSkillLevel(player, categoryId, skillId);
    }

    /**
     * Get only the base (purchased/persisted) skill level.
     */
    public int getBaseSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return skillLevelingManager.getBaseSkillLevel(player, categoryId, skillId);
    }

    /**
     * @deprecated Use {@link #getTotalSkillLevel} or {@link #getBaseSkillLevel}
     */
    @Deprecated
    public int getSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return skillLevelingManager.getSkillLevel(player, categoryId, skillId);
    }

    /**
     * Advance a skill to the next level for a player
     */
    public boolean advanceSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return skillLevelingManager.advanceSkillLevel(player, categoryId, skillId, false);
    }

    /**
     * Advance a skill to the next level for a player (admin version - bypasses
     * checks)
     */
    public boolean advanceSkillLevelAdmin(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return skillLevelingManager.advanceSkillLevel(player, categoryId, skillId, true);
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