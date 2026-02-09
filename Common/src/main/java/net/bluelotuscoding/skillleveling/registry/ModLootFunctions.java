package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.loot.RandomizeSkillTomeLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModLootFunctions {
    public static LootFunctionType RANDOMIZE_SKILL_TOME;

    public static void register() {
        RANDOMIZE_SKILL_TOME = Registry.register(Registries.LOOT_FUNCTION_TYPE,
                SkillLevelingMod.createIdentifier("randomize_skill_tome"),
                new LootFunctionType(new RandomizeSkillTomeLootFunction.Serializer()));
    }
}
