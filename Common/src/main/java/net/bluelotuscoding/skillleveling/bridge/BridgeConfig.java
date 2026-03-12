package net.bluelotuscoding.skillleveling.bridge;

import java.util.HashMap;
import java.util.Map;

public class BridgeConfig {
    public boolean enabled = true;
    public Map<String, java.util.List<String>> classToCategoryMap = new HashMap<>();
    public Map<String, Boolean> categorySyncEnabled = new HashMap<>();
    public Map<String, String> statToSkillMap = new HashMap<>();
    public Map<String, String> classPassiveToSkillMap = new HashMap<>();
    public Map<String, Integer> classPassiveToLevelMap = new HashMap<>();
    public boolean autoActivateCategory = true;
    public boolean lockOtherCategories = true;
    public boolean syncOnLogin = true;
    public boolean disableBaseClasses = false;

    public BridgeConfig() {
        // Default mappings (category path only; namespace resolved at runtime)
        classToCategoryMap.put("WARRIOR", java.util.Collections.singletonList("warrior"));
        classToCategoryMap.put("PALADIN", java.util.Collections.singletonList("paladin"));
        classToCategoryMap.put("BERSERKER", java.util.Collections.singletonList("berserker"));
        classToCategoryMap.put("REAPER", java.util.Collections.singletonList("reaper"));
        classToCategoryMap.put("SORCERER", java.util.Collections.singletonList("sorcerer"));
        classToCategoryMap.put("ARCHER", java.util.Collections.singletonList("archer"));
        classToCategoryMap.put("NECROMANCER", java.util.Collections.singletonList("necromancer"));

        // Default sync flags (true for all mapped categories)
        categorySyncEnabled.put("warrior", true);
        categorySyncEnabled.put("paladin", true);
        categorySyncEnabled.put("berserker", true);
        categorySyncEnabled.put("reaper", true);
        categorySyncEnabled.put("sorcerer", true);
        categorySyncEnabled.put("archer", true);
        categorySyncEnabled.put("necromancer", true);

        // Passive skill mappings (slot indices 0-3)
        for (String className : classToCategoryMap.keySet()) {
            int[] levels = getLevelsForClass(className);
            for (int i = 0; i < 4; i++) {
                // By default, passive skills map to the first category in the list
                String firstCategory = classToCategoryMap.get(className).get(0);
                classPassiveToSkillMap.put(className + "_" + i, firstCategory + "_passive_" + i);
                classPassiveToLevelMap.put(className + "_" + i, levels[i]);
            }
        }
    }

    private int[] getLevelsForClass(String className) {
        return switch (className) {
            case "WARRIOR", "PALADIN" -> new int[] { 1, 5, 10, 20 };
            case "BERSERKER" -> new int[] { 0, 5, 10, 20 };
            default -> new int[] { 1, 5, 10, 15 };
        };
    }

    /**
     * Merges any missing default entries from a freshly-constructed BridgeConfig
     * into this config. Safe to call after Gson deserialization — preserves all
     * existing user entries, only adds keys that are completely absent.
     *
     * @return true if any entries were added (caller should re-save the file)
     */
    public boolean applyMissingDefaults() {
        BridgeConfig defaults = new BridgeConfig();
        boolean changed = false;
        for (var e : defaults.classToCategoryMap.entrySet()) {
            if (!classToCategoryMap.containsKey(e.getKey())) {
                classToCategoryMap.put(e.getKey(), e.getValue());
                changed = true;
            }
        }
        for (var e : defaults.categorySyncEnabled.entrySet()) {
            if (!categorySyncEnabled.containsKey(e.getKey())) {
                categorySyncEnabled.put(e.getKey(), e.getValue());
                changed = true;
            }
        }
        for (var e : defaults.classPassiveToSkillMap.entrySet()) {
            if (!classPassiveToSkillMap.containsKey(e.getKey())) {
                classPassiveToSkillMap.put(e.getKey(), e.getValue());
                changed = true;
            }
        }
        for (var e : defaults.classPassiveToLevelMap.entrySet()) {
            if (!classPassiveToLevelMap.containsKey(e.getKey())) {
                classPassiveToLevelMap.put(e.getKey(), e.getValue());
                changed = true;
            }
        }
        return changed;
    }
}
