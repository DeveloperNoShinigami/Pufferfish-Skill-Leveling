package net.bluelotuscoding.skillleveling.bridge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.bluelotuscoding.skillleveling.bridge.BridgeConfig;
import net.bluelotuscoding.skillleveling.bridge.BridgeConfigManager;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.Map;

public class BridgeDataLoader extends JsonDataLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public BridgeDataLoader() {
        super(GSON, "epicclassmod");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        prepared.forEach((id, element) -> {
            if (!id.getPath().equals("pufferfish_skills_bridge"))
                return;
            try {
                BridgeConfig bridgeConfig = GSON.fromJson(element, BridgeConfig.class);
                if (bridgeConfig != null) {
                    AddonLogger.LOGGER.info("Applying Epic Class bridge config from datapack: " + id);
                    BridgeConfigManager.setConfig(bridgeConfig);
                    EpicClassBridge.loadConfig(bridgeConfig);
                    
                    // SYNC TO ALL PLAYERS: Ensure clients receive updated config (e.g. disableBaseClasses)
                    // immediately upon /reload without needing to relog.
                    net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().syncBridgeToAll();
                }
            } catch (Exception e) {
                AddonLogger.LOGGER.error("Error loading bridge config from datapack " + id + ": " + e.getMessage());
            }
        });

        // Parse and cache passive skill descriptions and titles from the loaded datapack (definitions.json)
        EpicClassBridge.loadSkillDisplayData(manager);
    }
}
