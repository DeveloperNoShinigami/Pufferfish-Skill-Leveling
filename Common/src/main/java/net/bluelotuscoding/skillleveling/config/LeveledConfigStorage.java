package net.bluelotuscoding.skillleveling.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Identifier;

/**
 * Stores leveled configuration data parsed from skill definitions.
 * This is populated by SkillDefinitionConfigMixin and accessed by
 * SkillLevelingManager.
 */
public class LeveledConfigStorage {

    private static final Map<String, LeveledConfig> leveledConfigs = new HashMap<>();
    private static final Map<Identifier, List<RequiredSkillEntry>> categoryPrerequisites = new HashMap<>();
    private static final Map<Identifier, Boolean> categoryKeepUnlocked = new HashMap<>();

    public static void put(String skillId, LeveledConfig leveledConfig) {
        leveledConfigs.put(skillId, leveledConfig);
    }

    public static LeveledConfig get(String skillId) {
        if (skillId == null) {
            return null;
        }

        // 1. Exact match
        if (leveledConfigs.containsKey(skillId)) {
            return leveledConfigs.get(skillId);
        }

        // 2. Fuzzy match (ignore namespaces and handle potential path mismatches)
        String skillPath = skillId;
        if (skillId.contains(":")) {
            skillPath = skillId.substring(skillId.indexOf(':') + 1);
        }

        for (Map.Entry<String, LeveledConfig> entry : leveledConfigs.entrySet()) {
            String key = entry.getKey();
            String keyPath = key;
            if (key.contains(":")) {
                keyPath = key.substring(key.indexOf(':') + 1);
            }

            // More aggressive matching:
            // Match if:
            // - Paths are identical
            // - One is a suffix of the other (e.g. "offense/dragons_breath" matches
            // "dragons_breath" or vice versa)
            // - They match after stripping all slashes (fallback for some internal
            // Pufferfish IDs)
            if (skillPath.equals(keyPath) ||
                    skillPath.endsWith("/" + keyPath) ||
                    keyPath.endsWith("/" + skillPath) ||
                    skillPath.replace("/", "").equals(keyPath.replace("/", ""))) {
                return entry.getValue();
            }
        }

        return null;
    }

    public static boolean has(String skillId) {
        return get(skillId) != null;
    }

    public static Map<String, LeveledConfig> getAllEntries() {
        return new HashMap<>(leveledConfigs);
    }

    public static void clear() {
        leveledConfigs.clear();
        categoryPrerequisites.clear();
        categoryKeepUnlocked.clear();
    }

    public static void putCategoryPrerequisites(Identifier id, List<RequiredSkillEntry> prereqs) {
        categoryPrerequisites.put(id, prereqs);
    }

    public static void putCategoryPrerequisites(Identifier id, List<RequiredSkillEntry> prereqs, boolean keepUnlocked) {
        categoryPrerequisites.put(id, prereqs);
        categoryKeepUnlocked.put(id, keepUnlocked);
    }

    public static boolean isKeepUnlocked(Identifier id) {
        return categoryKeepUnlocked.getOrDefault(id, false);
    }

    public static List<RequiredSkillEntry> getCategoryPrerequisites(Identifier id) {
        return categoryPrerequisites.get(id);
    }

    public static java.util.Set<Identifier> getAllGatedCategories() {
        return categoryPrerequisites.keySet();
    }

    public static class LeveledConfig {
        public final int maxLevels;
        public final int pointsPerLevel;
        public final boolean mergeDescription;
        public final List<RequiredSkillEntry> requiredSkills;
        public final Map<Integer, List<RequiredSkillEntry>> requiredSkillsForLevel;
        public final String lootMode;
        public final String categoryId;
        public final EnchantmentCostConfig enchantmentCost;
        public final EnchantmentCostConfig imbuementCost;
        public final EnchantmentCostConfig slotOpeningCost;
        public final EnchantmentCostConfig cleansingCost;
        public final boolean isLootable;
        public final boolean hidden;
        public final boolean toggle;
        public final int keybindSlot;
        public final int cooldown;

        public LeveledConfig(int maxLevels, int pointsPerLevel, boolean mergeDescription, String lootMode,
                String categoryId, boolean isLootable, boolean hidden) {
            this(maxLevels, pointsPerLevel, mergeDescription, new ArrayList<>(), new HashMap<>(), lootMode, categoryId,
                    EnchantmentCostConfig.FREE, EnchantmentCostConfig.FREE, EnchantmentCostConfig.FREE,
                    EnchantmentCostConfig.FREE, isLootable, hidden, false, 0, 0);
        }

        public LeveledConfig(int maxLevels, int pointsPerLevel, boolean mergeDescription, String lootMode,
                String categoryId, EnchantmentCostConfig enchantmentCost, EnchantmentCostConfig imbuementCost,
                boolean isLootable, boolean hidden) {
            this(maxLevels, pointsPerLevel, mergeDescription, new ArrayList<>(), new HashMap<>(), lootMode, categoryId,
                    enchantmentCost,
                    imbuementCost, EnchantmentCostConfig.FREE, EnchantmentCostConfig.FREE, isLootable,
                    hidden, false, 0, 0);
        }

        public LeveledConfig(int maxLevels, int pointsPerLevel, boolean mergeDescription,
                List<RequiredSkillEntry> requiredSkills, Map<Integer, List<RequiredSkillEntry>> requiredSkillsForLevel,
                String lootMode, String categoryId,
                EnchantmentCostConfig enchantmentCost, EnchantmentCostConfig imbuementCost,
                EnchantmentCostConfig slotOpeningCost, EnchantmentCostConfig cleansingCost, boolean isLootable,
                boolean hidden, boolean toggle, int keybindSlot, int cooldown) {
            this.maxLevels = maxLevels;
            this.pointsPerLevel = pointsPerLevel;
            this.mergeDescription = mergeDescription;
            this.requiredSkills = requiredSkills != null ? requiredSkills : new ArrayList<>();
            this.requiredSkillsForLevel = requiredSkillsForLevel != null ? requiredSkillsForLevel : new HashMap<>();
            this.lootMode = lootMode;
            this.categoryId = categoryId;
            this.enchantmentCost = enchantmentCost != null ? enchantmentCost : EnchantmentCostConfig.FREE;
            this.imbuementCost = imbuementCost != null ? imbuementCost : this.enchantmentCost;
            this.slotOpeningCost = slotOpeningCost != null ? slotOpeningCost : EnchantmentCostConfig.FREE;
            this.cleansingCost = cleansingCost != null ? cleansingCost : EnchantmentCostConfig.FREE;
            this.isLootable = isLootable;
            this.hidden = hidden;
            this.toggle = toggle;
            this.keybindSlot = keybindSlot;
            this.cooldown = cooldown;
        }
    }

    public static class EnchantmentCostConfig {
        public static final EnchantmentCostConfig FREE = new EnchantmentCostConfig();

        public enum Type {
            FREE, SCALAR, ARRAY, EXPRESSION
        }

        public final Type type;
        private int scalarValue = 0;
        private int[] arrayValues = null;
        private net.puffish.skillsmod.expression.Expression<Double> expression = null;

        private EnchantmentCostConfig() {
            this.type = Type.FREE;
        }

        public EnchantmentCostConfig(int scalar) {
            this.type = Type.SCALAR;
            this.scalarValue = scalar;
        }

        public EnchantmentCostConfig(int[] array) {
            this.type = Type.ARRAY;
            this.arrayValues = array;
        }

        public EnchantmentCostConfig(String expressionStr) {
            this.type = Type.EXPRESSION;
            var result = net.puffish.skillsmod.expression.DefaultParser.parse(expressionStr, java.util.Set.of("level"));
            this.expression = result.getSuccess().orElse(null);
        }

        public int getCost(int level) {
            switch (type) {
                case SCALAR:
                    return scalarValue * level;
                case ARRAY:
                    if (arrayValues == null || arrayValues.length == 0)
                        return 0;
                    int index = Math.max(0, Math.min(level - 1, arrayValues.length - 1));
                    return arrayValues[index];
                case EXPRESSION:
                    if (expression == null)
                        return 0;
                    Map<String, Double> vars = new HashMap<>();
                    vars.put("level", (double) level);
                    try {
                        return (int) Math.round(expression.eval(vars));
                    } catch (Exception e) {
                        return 0;
                    }
                default:
                    return 0;
            }
        }
    }

    public static class RequiredSkillEntry {
        public final String skillId;
        public final int minLevel;
        public final String categoryId; // null means same category

        public RequiredSkillEntry(String skillId, int minLevel) {
            this(skillId, minLevel, null);
        }

        public RequiredSkillEntry(String skillId, int minLevel, String categoryId) {
            this.skillId = skillId;
            this.minLevel = minLevel;
            this.categoryId = categoryId;
        }
    }
}
