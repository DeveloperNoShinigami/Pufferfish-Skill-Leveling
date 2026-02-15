package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.loot.RandomizeSkillTomeLootFunction;
import net.bluelotuscoding.skillleveling.loot.SkillImbueLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.registry.RegistryKeys;
import net.minecraftforge.registries.RegistryObject;

public class ForgeLootFunctionRegistry {
    public static final DeferredRegister<LootFunctionType> LOOT_FUNCTIONS = DeferredRegister
            .create(RegistryKeys.LOOT_FUNCTION_TYPE, SkillLevelingMod.MOD_ID);

    public static final RegistryObject<LootFunctionType> RANDOMIZE_SKILL_TOME = LOOT_FUNCTIONS.register(
            "randomize_skill_tome",
            () -> {
                LootFunctionType type = new LootFunctionType(new RandomizeSkillTomeLootFunction.Serializer());
                ModLootFunctions.RANDOMIZE_SKILL_TOME = type; // Inject into common
                return type;
            });

    public static final RegistryObject<LootFunctionType> SKILL_IMBUE = LOOT_FUNCTIONS.register("skill_imbue",
            () -> {
                LootFunctionType type = new LootFunctionType(new SkillImbueLootFunction.Serializer());
                ModLootFunctions.SKILL_IMBUE = type; // Inject into common
                return type;
            });

    public static void register(IEventBus bus) {
        LOOT_FUNCTIONS.register(bus);
    }
}
