package net.bluelotuscoding.skillleveling.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
import net.bluelotuscoding.skillleveling.item.SkillTomeItem;
import net.bluelotuscoding.skillleveling.registry.ModItems;
import net.bluelotuscoding.skillleveling.util.SkillResolver;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.util.JsonHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomizeSkillTomeLootFunction extends ConditionalLootFunction {
    private final int minLevel;
    private final int maxLevel;
    private final String skill;
    private final List<UnifiedLootConfig.SkillPoolEntry> skills;

    protected RandomizeSkillTomeLootFunction(LootCondition[] conditions, int minLevel, int maxLevel, String skill,
            List<UnifiedLootConfig.SkillPoolEntry> skills) {
        super(conditions);
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.skill = skill;
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    @Override
    public LootFunctionType getType() {
        return net.bluelotuscoding.skillleveling.registry.ModLootFunctions.RANDOMIZE_SKILL_TOME;
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        String targetSkillId = null;

        // 1. Specific skill
        if (this.skill != null && !this.skill.isEmpty()) {
            targetSkillId = this.skill;
        }
        // 2. Weighted skill pool
        else if (!this.skills.isEmpty()) {
            int totalWeight = this.skills.stream().mapToInt(UnifiedLootConfig.SkillPoolEntry::weight).sum();
            if (totalWeight > 0) {
                int roll = context.getRandom().nextInt(totalWeight);
                int current = 0;
                for (UnifiedLootConfig.SkillPoolEntry entry : this.skills) {
                    current += entry.weight();
                    if (roll < current) {
                        targetSkillId = entry.skill();
                        break;
                    }
                }
            }
        }

        // If we identified a specific target skill, try to resolve and create it
        if (targetSkillId != null) {
            var resolved = SkillResolver.resolve(targetSkillId);
            if (resolved.isPresent()) {
                var res = resolved.get();
                var config = res.config();
                int effectiveMax = Math.min(maxLevel, config.maxLevels);
                int effectiveMin = Math.min(minLevel, effectiveMax);

                int level = effectiveMin;
                if (effectiveMax > effectiveMin) {
                    level += context.getRandom().nextInt(effectiveMax - effectiveMin + 1);
                }
                level = Math.max(1, level);

                ItemStack tome = SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, res.categoryId(), res.fullId(),
                        config.lootMode, level);
                stack.setNbt(tome.getNbt());
                return stack;
            }
        }

        // 3. Fallback: Random lootable skill
        var allSkills = LeveledConfigStorage.getAllEntries();
        if (!allSkills.isEmpty()) {
            var entries = new ArrayList<>(allSkills.entrySet());
            Collections.shuffle(entries, new java.util.Random(context.getRandom().nextLong()));
            for (var entry : entries) {
                var config = entry.getValue();
                if (config.isLootable && config.lootMode != null && config.categoryId != null) {
                    int effectiveMax = Math.min(maxLevel, config.maxLevels);
                    int effectiveMin = Math.min(minLevel, effectiveMax);

                    int level = effectiveMin;
                    if (effectiveMax > effectiveMin) {
                        level += context.getRandom().nextInt(effectiveMax - effectiveMin + 1);
                    }
                    level = Math.max(1, level);

                    ItemStack tome = SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, config.categoryId,
                            entry.getKey(), config.lootMode, level);
                    stack.setNbt(tome.getNbt());
                    return stack;
                }
            }
        }

        return stack;
    }

    public static Builder builder(int minLevel, int maxLevel) {
        return new Builder(minLevel, maxLevel);
    }

    public static Builder builder(String skill, int minLevel, int maxLevel) {
        return new Builder(minLevel, maxLevel).withSkill(skill);
    }

    public static class Builder extends ConditionalLootFunction.Builder<Builder> {
        private final int minLevel;
        private final int maxLevel;
        private String skill = null;
        private List<UnifiedLootConfig.SkillPoolEntry> skills = new ArrayList<>();

        public Builder(int minLevel, int maxLevel) {
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }

        public Builder withSkill(String skill) {
            this.skill = skill;
            return this;
        }

        public Builder withSkills(List<UnifiedLootConfig.SkillPoolEntry> skills) {
            this.skills = skills;
            return this;
        }

        @Override
        protected Builder getThisBuilder() {
            return this;
        }

        @Override
        public net.minecraft.loot.function.LootFunction build() {
            return new RandomizeSkillTomeLootFunction(this.getConditions(), minLevel, maxLevel, skill, skills);
        }
    }

    public static class Serializer extends ConditionalLootFunction.Serializer<RandomizeSkillTomeLootFunction> {
        @Override
        public void toJson(JsonObject json, RandomizeSkillTomeLootFunction function, JsonSerializationContext context) {
            super.toJson(json, function, context);
            json.addProperty("min_level", function.minLevel);
            json.addProperty("max_level", function.maxLevel);
            if (function.skill != null) {
                json.addProperty("skill", function.skill);
            }
            if (!function.skills.isEmpty()) {
                JsonArray arr = new JsonArray();
                for (UnifiedLootConfig.SkillPoolEntry e : function.skills) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("skill", e.skill());
                    obj.addProperty("weight", e.weight());
                    arr.add(obj);
                }
                json.add("skills", arr);
            }
        }

        @Override
        public RandomizeSkillTomeLootFunction fromJson(JsonObject json, JsonDeserializationContext context,
                LootCondition[] conditions) {
            int minLevel = JsonHelper.getInt(json, "min_level", 1);
            int maxLevel = JsonHelper.getInt(json, "max_level", 1);
            String skill = JsonHelper.hasString(json, "skill") ? JsonHelper.getString(json, "skill") : null;
            List<UnifiedLootConfig.SkillPoolEntry> skills = new ArrayList<>();
            if (json.has("skills")) {
                JsonArray arr = json.getAsJsonArray("skills");
                for (JsonElement e : arr) {
                    JsonObject obj = e.getAsJsonObject();
                    skills.add(new UnifiedLootConfig.SkillPoolEntry(
                            JsonHelper.getString(obj, "skill"),
                            JsonHelper.getInt(obj, "weight")));
                }
            }
            return new RandomizeSkillTomeLootFunction(conditions, minLevel, maxLevel, skill, skills);
        }
    }
}
