package net.bluelotuscoding.skillleveling.main;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.registry.ModItems;
import net.bluelotuscoding.skillleveling.commands.SkillLevelingCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

public class FabricMain implements ModInitializer {

        public static final RegistryKey<ItemGroup> BASE_TOMES_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP,
                        SkillLevelingMod.createIdentifier("base_tomes"));

        public static final ItemGroup BASE_TOMES_GROUP = FabricItemGroup.builder()
                        .icon(() -> new ItemStack(ModItems.TOME_OF_PROGRESSION))
                        .displayName(Text.translatable("itemGroup.puffish_skill_leveling_base"))
                        .entries((context, entries) -> {
                                ModItems.fillBaseTomesTab(entries::add);
                        })
                        .build();

        public static final ItemGroup SKILL_TOMES_GROUP = FabricItemGroup.builder()
                        .icon(() -> new ItemStack(ModItems.SKILL_TOME))
                        .displayName(Text.translatable("itemGroup.puffish_skill_leveling_tomes"))
                        .entries((context, entries) -> {
                                ModItems.fillSkillTomesTab(entries::add);
                        })
                        .build();

        @Override
        public void onInitialize() {
                // Initialize common addon logic
                SkillLevelingMod.init();

                // Register items
                ModItems.TOME_OF_PROGRESSION = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_PROGRESSION_ID,
                                ModItems.createTomeOfProgression());

                ModItems.TOME_OF_CLEAR_MIND = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_CLEAR_MIND_ID,
                                ModItems.createTomeOfClearMind());

                ModItems.TOME_OF_GREATER_CLEAR_MIND = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_GREATER_CLEAR_MIND_ID,
                                ModItems.createTomeOfGreaterClearMind());

                ModItems.SKILL_TOME = Registry.register(
                                Registries.ITEM,
                                ModItems.SKILL_TOME_ID,
                                ModItems.createSkillTome());

                ModItems.SIGIL_OF_IMBUEMENT = Registry.register(
                                Registries.ITEM,
                                ModItems.SIGIL_OF_IMBUEMENT_ID,
                                ModItems.createSigilOfImbuement());

                ModItems.TOME_OF_CLEANSING = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_OF_CLEANSING_ID,
                                ModItems.createTomeOfCleansing());

                ModItems.TOME_OF_CLEANSING_2 = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_OF_CLEANSING_2_ID,
                                ModItems.createTomeOfCleansing2());

                ModItems.TOME_OF_CLEANSING_3 = Registry.register(
                                Registries.ITEM,
                                ModItems.TOME_OF_CLEANSING_3_ID,
                                ModItems.createTomeOfCleansing3());

                // Register groups
                Registry.register(Registries.ITEM_GROUP, BASE_TOMES_KEY, BASE_TOMES_GROUP);
                Registry.register(Registries.ITEM_GROUP, SkillLevelingMod.createIdentifier("skill_tomes"),
                                SKILL_TOMES_GROUP);

                // Register commands
                CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                        SkillLevelingCommand.register(dispatcher);
                });
        }
}
