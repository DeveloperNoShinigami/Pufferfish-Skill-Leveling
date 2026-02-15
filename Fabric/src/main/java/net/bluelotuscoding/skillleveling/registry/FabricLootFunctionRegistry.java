package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.loot.RandomizeSkillTomeLootFunction;
import net.bluelotuscoding.skillleveling.loot.SkillImbueLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class FabricLootFunctionRegistry {

    public static void register() {
        ModLootFunctions.RANDOMIZE_SKILL_TOME = Registry.register(Registries.LOOT_FUNCTION_TYPE,
                ModLootFunctions.RANDOMIZE_SKILL_TOME_ID,
                new LootFunctionType(new RandomizeSkillTomeLootFunction.Serializer()));

        ModLootFunctions.SKILL_IMBUE = Registry.register(Registries.LOOT_FUNCTION_TYPE,
                ModLootFunctions.SKILL_IMBUE_ID,
                new LootFunctionType(new SkillImbueLootFunction.Serializer()));
    }
}
