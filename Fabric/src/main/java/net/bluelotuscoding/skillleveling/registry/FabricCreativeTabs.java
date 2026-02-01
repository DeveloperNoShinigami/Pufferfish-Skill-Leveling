package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

public class FabricCreativeTabs {

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

    public static void register() {
        Registry.register(Registries.ITEM_GROUP, BASE_TOMES_KEY, BASE_TOMES_GROUP);
        Registry.register(Registries.ITEM_GROUP, SkillLevelingMod.createIdentifier("skill_tomes"),
                SKILL_TOMES_GROUP);
    }
}
