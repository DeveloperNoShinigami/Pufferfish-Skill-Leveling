package net.bluelotuscoding.skillleveling.bridge;

import java.util.HashMap;
import java.util.Map;

public class BridgeConfig {
    public boolean enabled = true;
    public Map<String, String> classToCategoryMap = new HashMap<>();
    public boolean autoActivateCategory = true;
    public boolean lockOtherCategories = true;
    public boolean syncOnLogin = true;

    public BridgeConfig() {
        // Default mappings (category path only; namespace resolved at runtime)
        classToCategoryMap.put("WARRIOR", "warrior");
        classToCategoryMap.put("PALADIN", "paladin");
        classToCategoryMap.put("BERSERKER", "berserker");
        classToCategoryMap.put("REAPER", "reaper");
        classToCategoryMap.put("SORCERER", "sorcerer");
        classToCategoryMap.put("ARCHER", "archer");
    }
}
