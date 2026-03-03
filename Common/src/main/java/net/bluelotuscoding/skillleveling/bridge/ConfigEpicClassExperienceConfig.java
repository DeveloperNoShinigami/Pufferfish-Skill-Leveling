package net.bluelotuscoding.skillleveling.bridge;

import java.util.HashSet;
import java.util.Set;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.util.Identifier;

public class ConfigEpicClassExperienceConfig implements EpicClassExperienceConfig {
    private static final Set<String> WARNED_CATEGORY_IDS = new HashSet<>();
    private static boolean warnedConfigNotLoaded = false;

    @Override
    public boolean isSyncEnabled(Identifier categoryId) {
        if (categoryId == null) {
            return true;
        }

        if (!BridgeConfigManager.isLoaded()) {
            logConfigNotLoaded();
            return true;
        }

        BridgeConfig config = BridgeConfigManager.getConfig();
        if (config == null || config.categorySyncEnabled == null) {
            logConfigNotLoaded();
            return true;
        }

        String fullId = categoryId.toString();
        Boolean fullValue = config.categorySyncEnabled.get(fullId);
        if (fullValue != null) {
            return fullValue;
        }

        String pathOnly = categoryId.getPath();
        Boolean pathValue = config.categorySyncEnabled.get(pathOnly);
        if (pathValue != null) {
            return pathValue;
        }

        logMissingCategory(categoryId);
        return true;
    }

    private static void logConfigNotLoaded() {
        if (warnedConfigNotLoaded) {
            return;
        }
        warnedConfigNotLoaded = true;
        if (SkillLevelingMod.getInstance() != null) {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("Epic Class sync config not loaded; defaulting sync to true.");
        }
    }

    private static void logMissingCategory(Identifier categoryId) {
        String key = categoryId.toString();
        if (!WARNED_CATEGORY_IDS.add(key)) {
            return;
        }
        if (SkillLevelingMod.getInstance() != null) {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("No sync flag for category " + key
                            + " in pufferfish_skills_bridge.json; defaulting to true.");
        }
    }
}
