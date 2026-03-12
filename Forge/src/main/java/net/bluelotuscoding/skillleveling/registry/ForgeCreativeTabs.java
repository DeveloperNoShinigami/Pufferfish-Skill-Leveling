package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ForgeCreativeTabs {
        public static final DeferredRegister<ItemGroup> CREATIVE_TABS = DeferredRegister.create(
                        Registries.ITEM_GROUP.getKey(),
                        SkillLevelingMod.MOD_ID);

        public static final RegistryObject<ItemGroup> BASE_TOMES_TAB = CREATIVE_TABS.register("base_tomes_tab",
                        () -> ItemGroup.builder()
                                        .icon(() -> new ItemStack(ModItems.TOME_OF_PROGRESSION))
                                        .displayName(Text.translatable("itemGroup.puffish_skill_leveling_base"))
                                        .entries((displayContext, entries) -> ModItems.fillBaseTomesTab(entries::add))
                                        .build());

        public static final RegistryObject<ItemGroup> SKILL_TOMES_TAB = CREATIVE_TABS.register("skill_tomes_tab",
                        () -> ItemGroup.builder()
                                        .withTabsBefore(BASE_TOMES_TAB.getId())
                                        .icon(() -> new ItemStack(ModItems.SKILL_TOME))
                                        .displayName(Text.translatable("itemGroup.puffish_skill_leveling_tomes"))
                                        .entries((displayContext, entries) -> ModItems.fillSkillTomesTab(entries::add))
                                        .build());

        public static final RegistryObject<ItemGroup> EXP_TOMES_TAB = CREATIVE_TABS.register("exp_tomes_tab",
                        () -> ItemGroup.builder()
                                        .withTabsBefore(SKILL_TOMES_TAB.getId())
                                        .icon(() -> new ItemStack(ModItems.EXP_TOME))
                                        .displayName(Text.translatable("itemGroup.puffish_skill_leveling_exp_tomes"))
                                        .entries((displayContext, entries) -> ModItems.fillExpTomesTab(entries::add))
                                        .build());

        public static void register(IEventBus bus) {
                CREATIVE_TABS.register(bus);
        }
}
