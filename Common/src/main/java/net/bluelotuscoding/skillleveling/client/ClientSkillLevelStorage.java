package net.bluelotuscoding.skillleveling.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CLIENT-SIDE SKILL LEVEL STORAGE
 * 
 * Stores skill levels synchronized from the server for display in the UI.
 */
public class ClientSkillLevelStorage {
    private static final Map<String, Integer> baseSkillLevels = new ConcurrentHashMap<>();
    private static final Map<String, Integer> totalSkillLevels = new ConcurrentHashMap<>();
    private static final Map<String, Integer> skillMaxLevels = new ConcurrentHashMap<>();
    private static final Map<String, Integer> skillPointsPerLevel = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> hiddenSkillFlags = new ConcurrentHashMap<>();

    // Mapping from category path to the last seen full category ID (for shorthand
    // resolution)
    private static final Map<String, String> pathToFullCategory = new ConcurrentHashMap<>();

    // Mapping from definition ID to the key (for mixin lookups)
    private static final Map<String, String> definitionToKey = new ConcurrentHashMap<>();

    private static String getKey(String categoryId, String skillId) {
        if (categoryId == null || skillId == null) {
            return "null:null";
        }

        // NORMALIZE CATEGORY ID: Use standard Identifier logic for normalization.
        // We prefer the full namespaced ID if we've seen it before (dynamic
        // resolution).
        String resolvedCategory = categoryId;
        if (!categoryId.contains(":")) {
            // Shorthand ID: try to find a known full ID that matches this path
            resolvedCategory = pathToFullCategory.getOrDefault(categoryId, categoryId);
        } else {
            try {
                net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(categoryId);
                if (id != null && id.getNamespace().equals("minecraft")) {
                    // "minecraft" namespace often means shorthand in NBT/config
                    resolvedCategory = pathToFullCategory.getOrDefault(id.getPath(), categoryId);
                }
            } catch (Exception e) {
                // Not a valid identifier, use as-is
            }
        }

        return resolvedCategory + ":" + skillId;
    }

    public static void setLevel(String categoryId, String skillId, int baseLevel, int totalLevel, int maxLevel,
            int pointsPerLevel, boolean hidden) {
        // "Learn" the full category ID from current packet
        if (categoryId != null && categoryId.contains(":")) {
            try {
                net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(categoryId);
                if (id != null) {
                    pathToFullCategory.put(id.getPath(), categoryId);
                }
            } catch (Exception ignored) {
            }
        }

        String key = getKey(categoryId, skillId);
        if (baseLevel <= 0 && totalLevel <= 0) {
            // Level 0 means skill is locked/reset - remove most info, but KEEP hidden
            // status
            // if we want to support hiding it until prerequisites are met.
            baseSkillLevels.remove(key);
            totalSkillLevels.remove(key);
            skillMaxLevels.remove(key);
            skillPointsPerLevel.remove(key);
        } else {
            baseSkillLevels.put(key, baseLevel);
            totalSkillLevels.put(key, totalLevel);
            skillMaxLevels.put(key, maxLevel);
            skillPointsPerLevel.put(key, pointsPerLevel);
        }
        // Always store hidden status if provided
        hiddenSkillFlags.put(key, hidden);
    }

    /**
     * Register a mapping from definition ID to skill key.
     * Called when syncing descriptions so we can look up levels by definition.
     */
    public static void registerDefinitionMapping(String definitionId, String categoryId, String skillId) {
        definitionToKey.put(definitionId, getKey(categoryId, skillId));
    }

    /**
     * Get level by definition ID (used by ClientSkillDefinitionConfigMixin).
     * Returns 0 if no mapping found.
     */
    public static int getLevelByDefinitionId(String definitionId) {
        String key = definitionToKey.get(definitionId);
        if (key == null) {
            return 0;
        }
        return baseSkillLevels.getOrDefault(key, 0);
    }

    /**
     * Get TOTAL level by definition ID.
     */
    public static int getTotalLevelByDefinitionId(String definitionId) {
        String key = definitionToKey.get(definitionId);
        if (key == null) {
            return 0;
        }
        return totalSkillLevels.getOrDefault(key, 0);
    }

    /**
     * Get max level by definition ID.
     */
    public static int getMaxLevelByDefinitionId(String definitionId) {
        String key = definitionToKey.get(definitionId);
        if (key == null) {
            return 0; // Changed default from 1 to 0 for better leveled check
        }
        return skillMaxLevels.getOrDefault(key, 0);
    }

    /**
     * Check if a skill definition is a leveled skill (max level > 1)
     */
    public static boolean isLeveledByDefinitionId(String definitionId) {
        return getMaxLevelByDefinitionId(definitionId) > 1;
    }

    /**
     * Get points per level by definition ID.
     */
    public static int getPointsPerLevelByDefinitionId(String definitionId) {
        String key = definitionToKey.get(definitionId);
        if (key == null) {
            return 0; // Means not a leveled skill in this context
        }
        return skillPointsPerLevel.getOrDefault(key, 0);
    }

    public static int getLevel(String categoryId, String skillId) {
        // Default to 0 (not unlocked)
        return totalSkillLevels.getOrDefault(getKey(categoryId, skillId), 0);
    }

    public static int getBaseLevel(String categoryId, String skillId) {
        return baseSkillLevels.getOrDefault(getKey(categoryId, skillId), 0);
    }

    public static int getMaxLevel(String categoryId, String skillId) {
        return skillMaxLevels.getOrDefault(getKey(categoryId, skillId), 1);
    }

    public static boolean isHidden(String categoryId, String skillId) {
        return hiddenSkillFlags.getOrDefault(getKey(categoryId, skillId), false);
    }

    public static boolean hasLevelInfo(String categoryId, String skillId) {
        return totalSkillLevels.containsKey(getKey(categoryId, skillId));
    }

    /**
     * Clear level info for a specific skill (used when skill is reset/locked).
     */
    public static void clearSkill(String categoryId, String skillId) {
        String key = getKey(categoryId, skillId);
        baseSkillLevels.remove(key);
        totalSkillLevels.remove(key);
        skillMaxLevels.remove(key);
    }

    /**
     * Clear all level info for a category (used when category is reset).
     */
    public static void clearCategory(String categoryId) {
        String prefix = categoryId + ":";
        baseSkillLevels.keySet().removeIf(k -> k.startsWith(prefix));
        totalSkillLevels.keySet().removeIf(k -> k.startsWith(prefix));
        skillMaxLevels.keySet().removeIf(k -> k.startsWith(prefix));
        hiddenSkillFlags.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /**
     * Calculate equipment bonus on the client side.
     */
    public static int getEquipmentBonus(String categoryId, String skillId) {
        Object playerObj = SideSafeClient.getPlayer();
        if (!(playerObj instanceof net.minecraft.entity.player.PlayerEntity player)) {
            return 0;
        }

        int bonus = 0;
        for (var slot : net.minecraft.entity.EquipmentSlot.values()) {
            var stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) {
                continue;
            }

            // Support new multi-slot format
            var skills = net.bluelotuscoding.skillleveling.util.ImbuedSkillHelper.getSkills(stack);
            if (!skills.isEmpty()) {
                for (var skill : skills) {
                    // Path-aware matching for equipment: allows shorthand category in NBT
                    // to match a full namespaced category in the tree.
                    boolean skillMatch = skill.skillId.equals(skillId);
                    boolean catMatch = skill.categoryId.equals(categoryId);

                    if (!catMatch && !skill.categoryId.contains(":")) {
                        // Try matching only paths if the item has no namespace
                        try {
                            net.minecraft.util.Identifier treeId = net.minecraft.util.Identifier.tryParse(categoryId);
                            if (treeId != null && treeId.getPath().equals(skill.categoryId)) {
                                catMatch = true;
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    if (skillMatch && catMatch) {
                        bonus += skill.level;
                    }
                }
            } else {
                // FALLBACK: Support old single-slot format
                var nbt = stack.getNbt();
                if (nbt != null && nbt.contains("SkillLevelingImbued", 10)) {
                    var imbueNbt = nbt.getCompound("SkillLevelingImbued");
                    String imbueCategory = imbueNbt.getString("CategoryId");
                    String imbueSkill = imbueNbt.getString("SkillId");

                    if (imbueCategory != null && !imbueCategory.isEmpty()) {
                        if (imbueCategory.equals(categoryId) && skillId.equals(imbueSkill)) {
                            int level = imbueNbt.contains("Level") ? imbueNbt.getInt("Level") : 1;
                            bonus += level;
                        }
                    }
                }
            }
        }
        return bonus;
    }

    /**
     * Get equipment bonus by definition ID.
     */
    public static int getEquipmentBonusByDefinitionId(String definitionId) {
        String key = definitionToKey.get(definitionId);
        if (key == null) {
            return 0;
        }
        String[] parts = key.split(":", 2);
        if (parts.length == 2) {
            return getEquipmentBonus(parts[0], parts[1]);
        }
        return 0;
    }

    /**
     * Clear all level info (used on disconnect/logout).
     */
    public static void clearAll() {
        baseSkillLevels.clear();
        totalSkillLevels.clear();
        skillMaxLevels.clear();
        skillPointsPerLevel.clear();
        hiddenSkillFlags.clear();
        definitionToKey.clear();
    }
}
