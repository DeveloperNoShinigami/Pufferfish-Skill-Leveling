package net.bluelotuscoding.skillleveling.bridge.forge;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class EpicClassBridgeForgeLoader {
    private static boolean initialized = false;

    private EpicClassBridgeForgeLoader() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (!ModList.get().isLoaded("epicclassmod")) {
            SkillLevelingMod.getInstance().getLogger().info("Epic Class Mod not found; bridge disabled");
            return;
        }

        if (!EpicClassBridge.isEnabled()) {
            SkillLevelingMod.getInstance().getLogger().info("Epic Class bridge config disabled");
            return;
        }

        MinecraftForge.EVENT_BUS.register(new EpicClassBridgeForgeEvents());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.register(new ClassBookScreenRenderer());
        }
        MinecraftForge.EVENT_BUS.register(new PlayerCleanupListener());
        SkillLevelingMod.getInstance().getLogger().info("Epic Class bridge enabled");
    }
}
