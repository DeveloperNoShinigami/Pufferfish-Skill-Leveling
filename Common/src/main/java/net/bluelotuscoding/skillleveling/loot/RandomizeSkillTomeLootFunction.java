package net.bluelotuscoding.skillleveling.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
import net.bluelotuscoding.skillleveling.item.SkillTomeItem;
import net.bluelotuscoding.skillleveling.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.util.JsonHelper;

import java.util.ArrayList;
import java.util.Collections;

public class RandomizeSkillTomeLootFunction extends ConditionalLootFunction {
    private final int minLevel;
    private final int maxLevel;

    protected RandomizeSkillTomeLootFunction(LootCondition[] conditions, int minLevel, int maxLevel) {
        super(conditions);
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    @Override
    public LootFunctionType getType() {
        return net.bluelotuscoding.skillleveling.registry.ModLootFunctions.RANDOMIZE_SKILL_TOME;
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        var entries = new ArrayList<>(LeveledConfigStorage.getAllEntries().entrySet());
        if (entries.isEmpty())
            return stack;

        Collections.shuffle(entries, new java.util.Random(context.getRandom().nextLong()));
        for (var entry : entries) {
            var config = entry.getValue();
            if (config.lootMode != null && config.categoryId != null) {
                // Any skill with a defined loot mode is "lootable" via tomes
                int level = minLevel + context.getRandom().nextInt(maxLevel - minLevel + 1);
                level = Math.max(1, Math.min(level, config.maxLevels));

                // Populate NBT of the existing stack
                ItemStack tome = SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, config.categoryId, entry.getKey(),
                        config.lootMode, level);
                stack.setNbt(tome.getNbt());
                return stack;
            }
        }
        return stack;
    }

    public static Builder builder(int minLevel, int maxLevel) {
        return new Builder(minLevel, maxLevel);
    }

    public static class Builder extends ConditionalLootFunction.Builder<Builder> {
        private final int minLevel;
        private final int maxLevel;

        public Builder(int minLevel, int maxLevel) {
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }

        @Override
        protected Builder getThisBuilder() {
            return this;
        }

        @Override
        public net.minecraft.loot.function.LootFunction build() {
            return new RandomizeSkillTomeLootFunction(this.getConditions(), minLevel, maxLevel);
        }
    }

    public static class Serializer extends ConditionalLootFunction.Serializer<RandomizeSkillTomeLootFunction> {
        @Override
        public void toJson(JsonObject json, RandomizeSkillTomeLootFunction function, JsonSerializationContext context) {
            super.toJson(json, function, context);
            json.addProperty("min_level", function.minLevel);
            json.addProperty("max_level", function.maxLevel);
        }

        @Override
        public RandomizeSkillTomeLootFunction fromJson(JsonObject json, JsonDeserializationContext context,
                LootCondition[] conditions) {
            int minLevel = JsonHelper.getInt(json, "min_level", 1);
            int maxLevel = JsonHelper.getInt(json, "max_level", 1);
            return new RandomizeSkillTomeLootFunction(conditions, minLevel, maxLevel);
        }
    }
}
