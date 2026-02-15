package net.bluelotuscoding.skillleveling.registry;

import com.mojang.serialization.Codec;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.forge.loot.SkillImbueLootModifier;
import net.bluelotuscoding.skillleveling.forge.loot.UniversalLootModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for Forge Global Loot Modifiers.
 */
public class ForgeLootModifierRegistry {
        public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister
                        .create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, SkillLevelingMod.MOD_ID);

        static {
                System.out.println("[ForgeLootModifierRegistry] Static initializer triggered.");
        }

        public static final RegistryObject<Codec<? extends IGlobalLootModifier>> SKILL_IMBUE = LOOT_MODIFIERS
                        .register("skill_imbue", () -> SkillImbueLootModifier.CODEC);

        public static final RegistryObject<Codec<? extends IGlobalLootModifier>> UNIVERSAL_LOOT = LOOT_MODIFIERS
                        .register("universal_loot", () -> UniversalLootModifier.CODEC);

        public static void register(IEventBus bus) {
                LOOT_MODIFIERS.register(bus);
        }
}
