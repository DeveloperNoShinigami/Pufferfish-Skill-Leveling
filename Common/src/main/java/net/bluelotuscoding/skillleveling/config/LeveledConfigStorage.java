package net.bluelotuscoding.skillleveling.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores leveled configuration data parsed from skill definitions.
 * This is populated by SkillDefinitionConfigMixin and accessed by
 * SkillLevelingManager.
 */
public class LeveledConfigStorage {

    private static final Map<String, LeveledConfig> leveledConfigs = new HashMap<>();

    public static void put(String skillId, LeveledConfig leveledConfig) {
        leveledConfigs.put(skillId, leveledConfig);
    }

    public static LeveledConfig get(String skillId) {
        if (skillId == null)
            return null;

        // Direct hit
        if (leveledConfigs.containsKey(skillId)) {
            return leveledConfigs.get(skillId);
        }

        // Fallback: check if any key ends with this ID (handles
        // namespace:category:skill ID variants)
        String suffix = ":" + skillId;
        for (Map.Entry<String, LeveledConfig> entry : leveledConfigs.entrySet()) {
            if (entry.getKey().endsWith(suffix)) {
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
    }

    public static class LeveledConfig {
        public final int maxLevels;
        public final int pointsPerLevel;
        public final boolean mergeDescription;
        public final List<RequiredSkillEntry> requiredSkills;
        public final String lootMode;
        public final String categoryId;
        public final EnchantmentCostConfig enchantmentCost;
        public final EnchantmentCostConfig imbuementCost;
        public final EnchantmentCostConfig slotOpeningCost;
        public final EnchantmentCostConfig cleansingCost;

        public LeveledConfig(int maxLevels, int pointsPerLevel, boolean mergeDescription, String lootMode,
                String categoryId) {
            this(maxLevels, pointsPerLevel, mergeDescription, new ArrayList<>(), lootMode, categoryId,
                    EnchantmentCostConfig.FREE, EnchantmentCostConfig.FREE, EnchantmentCostConfig.FREE,
                    EnchantmentCostConfig.FREE);
        }

        public LeveledConfig(int maxLevels, int pointsPerLevel, boolean mergeDescription, String lootMode,
                String categoryId, EnchantmentCostConfig enchantmentCost, EnchantmentCostConfig imbuementCost) {
            this(maxLevels, pointsPerLevel, mergeDescription, new ArrayList<>(), lootMode, categoryId, enchantmentCost,
                    imbuementCost, EnchantmentCostConfig.FREE, EnchantmentCostConfig.FREE);
        }

        public LeveledConfig(int maxLevels, int pointsPerLevel, boolean mergeDescription,
                List<RequiredSkillEntry> requiredSkills, String lootMode, String categoryId,
                EnchantmentCostConfig enchantmentCost, EnchantmentCostConfig imbuementCost,
                EnchantmentCostConfig slotOpeningCost, EnchantmentCostConfig cleansingCost) {
            this.maxLevels = maxLevels;
            this.pointsPerLevel = pointsPerLevel;
            this.mergeDescription = mergeDescription;
            this.requiredSkills = requiredSkills != null ? requiredSkills : new ArrayList<>();
            this.lootMode = lootMode;
            this.categoryId = categoryId;
            this.enchantmentCost = enchantmentCost != null ? enchantmentCost : EnchantmentCostConfig.FREE;
            this.imbuementCost = imbuementCost != null ? imbuementCost : this.enchantmentCost;
            this.slotOpeningCost = slotOpeningCost != null ? slotOpeningCost : EnchantmentCostConfig.FREE;
            this.cleansingCost = cleansingCost != null ? cleansingCost : EnchantmentCostConfig.FREE;
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

        public RequiredSkillEntry(String skillId, int minLevel) {
            this.skillId = skillId;
            this.minLevel = minLevel;
        }
    }
}
