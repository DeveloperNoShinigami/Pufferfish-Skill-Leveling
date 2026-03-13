package net.bluelotuscoding.skillleveling.bridge.forge.client.network;

import net.bluelotuscoding.skillleveling.bridge.BridgeConfig;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.bluelotuscoding.skillleveling.bridge.config.ClassPageDef;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
import net.bluelotuscoding.skillleveling.data.ExpTomeConfigLoader;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.player.PlayerEntity;
import net.bluelotuscoding.skillleveling.bridge.forge.client.screen.CustomClassSelectScreen;
import net.bluelotuscoding.skillleveling.client.ClientCustomClassState;
import net.bluelotuscoding.skillleveling.bridge.forge.ClientClassUIHelper;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Isolated handlers for client-only logic to prevent physical class leaks on dedicated servers.
 * These methods should only be called through side-safe checks.
 */
public class ForgeClientPacketHandlers {

    public static void handleSyncCustomNbt(NbtCompound nbt) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player != null && nbt != null) {
            player.getPersistentData().put("ecm_leveling", nbt);

            int level = nbt.getInt("level");
            int xp = nbt.getInt("xp");
            int xpNeeded = nbt.getInt("xp_needed");

            // Update our internal state for mixins to use
            net.bluelotuscoding.skillleveling.client.ClientCustomNbtState.update(level, xp, xpNeeded);

            // FORCE UI SYNC: Epic Class's ClientLevelState doesn't listen to persistent data changes.
            // We must force-reflect the new level and XP into its static fields.
            try {
                Class<?> cls = Class.forName("com.example.epicclassmod.client.ClientLevelState");
                
                java.lang.reflect.Field levelField = cls.getDeclaredField("level");
                levelField.setAccessible(true);
                levelField.setInt(null, level);
                
                java.lang.reflect.Field xpField = cls.getDeclaredField("xp");
                xpField.setAccessible(true);
                xpField.setInt(null, xp);

                java.lang.reflect.Field xpNeededField = cls.getDeclaredField("xpNeeded");
                xpNeededField.setAccessible(true);
                xpNeededField.setInt(null, xpNeeded);
            } catch (Exception e) {
                // If fields don't exist or class not found, we fallback to standard behavior
                AddonLogger.LOGGER.warn("Failed to reflect level to Epic Class UI: " + e.getMessage());
            }
        }
    }

    public static void handleOpenAdvanceClassScreen(String parentClassId) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            client.setScreen(new CustomClassSelectScreen(parentClassId));
        });
    }

    public static void handleCustomSyncClass(UUID playerId, String classId) {
        ClientCustomClassState.setCustomClass(playerId, classId);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && playerId.equals(mc.player.getUuid())) {
            ClientClassUIHelper.forceRefresh();
        }
    }

    public static void handleSyncBridgeContent(Map<String, EpicClassDef> classes,
            Map<String, java.util.List<net.bluelotuscoding.skillleveling.bridge.config.ClassPageDef>> pages,
            Map<String, String[]> skillDisplayCache,
            BridgeConfig config) {
        EpicClassConfigManager.setClassesOnClient(classes);
        EpicClassConfigManager.setAttributePagesOnClient(pages);
        EpicClassConfigManager.setSyncedConfig(config);
        EpicClassBridge.setSkillDisplayCache(skillDisplayCache);
        EpicClassBridge.loadConfig(config);
        EpicClassBridge.forceResolve();
        
        // Refresh creative tabs on client after sync
        refreshCreativeTabs();
    }

    private static void refreshCreativeTabs() {
        // In 1.20.1 Forge, creative tabs are usually static, but we can try to force a rebuild 
        // of the items if needed.
    }

    public static void handleSyncAllConfigs(Map<String, LeveledConfigStorage.LeveledConfig> leveledConfigs,
            Map<String, ExpTomeConfigLoader.ExpTomeDefinition> expTomeDefinitions) {
        AddonLogger.LOGGER.info("Received SyncAllConfigsPacket. Updating client-side configs.");
        LeveledConfigStorage.setAllOnClient(leveledConfigs);
        ExpTomeConfigLoader.setAllOnClient(expTomeDefinitions);

        // Refresh creative tabs on client after config sync
        refreshCreativeTabs();
    }
}
