package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.item.SigilOfImbuementItem;
import net.bluelotuscoding.skillleveling.item.SkillTomeItem;
import net.bluelotuscoding.skillleveling.item.TomeItem;
import net.bluelotuscoding.skillleveling.item.TomeOfCleansingItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ForgeItemRegistry {
        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
                        SkillLevelingMod.MOD_ID);
        public static final DeferredRegister<ItemGroup> CREATIVE_TABS = DeferredRegister
                        .create(Registries.ITEM_GROUP.getKey(), SkillLevelingMod.MOD_ID);

        // Items
        public static final RegistryObject<TomeItem> TOME_OF_PROGRESSION = ITEMS
                        .register("tome_of_progression", ModItems::createTomeOfProgression);
        public static final RegistryObject<TomeItem> TOME_OF_CLEAR_MIND = ITEMS
                        .register("tome_of_clear_mind", ModItems::createTomeOfClearMind);
        public static final RegistryObject<TomeItem> TOME_OF_GREATER_CLEAR_MIND = ITEMS
                        .register("tome_of_greater_clear_mind", ModItems::createTomeOfGreaterClearMind);
        public static final RegistryObject<SkillTomeItem> SKILL_TOME = ITEMS
                        .register("skill_tome", ModItems::createSkillTome);
        public static final RegistryObject<SigilOfImbuementItem> SIGIL_OF_IMBUEMENT = ITEMS
                        .register("sigil_of_imbuement", ModItems::createSigilOfImbuement);
        public static final RegistryObject<TomeOfCleansingItem> TOME_OF_CLEANSING = ITEMS
                        .register("tome_of_cleansing", ModItems::createTomeOfCleansing);
        public static final RegistryObject<TomeOfCleansingItem> TOME_OF_CLEANSING_2 = ITEMS
                        .register("tome_of_cleansing_2", ModItems::createTomeOfCleansing2);
        public static final RegistryObject<TomeOfCleansingItem> TOME_OF_CLEANSING_3 = ITEMS
                        .register("tome_of_cleansing_3", ModItems::createTomeOfCleansing3);

        // Block Items
        public static final RegistryObject<Item> SKILL_SCRIBE_TABLE_ITEM = ITEMS.register("skill_scribe_table",
                        () -> new BlockItem(ForgeBlockRegistry.SKILL_SCRIBE_TABLE.get(),
                                        new Item.Settings().rarity(Rarity.RARE)));

        // Creative Tabs
        public static final RegistryObject<ItemGroup> BASE_TOMES_TAB = CREATIVE_TABS.register("base_tomes_tab",
                        () -> ItemGroup.builder()
                                        .icon(() -> new ItemStack(TOME_OF_PROGRESSION.get()))
                                        .displayName(Text.translatable("itemGroup.puffish_skill_leveling_base"))
                                        .entries((displayContext, entries) -> ModItems.fillBaseTomesTab(entries::add))
                                        .build());

        public static final RegistryObject<ItemGroup> SKILL_TOMES_TAB = CREATIVE_TABS.register("skill_tomes_tab",
                        () -> ItemGroup.builder()
                                        .withTabsBefore(BASE_TOMES_TAB.getId())
                                        .icon(() -> new ItemStack(SKILL_TOME.get()))
                                        .displayName(Text.translatable("itemGroup.puffish_skill_leveling_tomes"))
                                        .entries((displayContext, entries) -> ModItems.fillSkillTomesTab(entries::add))
                                        .build());

        public static void register(IEventBus bus) {
                SkillLevelingMod.getInstance().getLogger().info("Initializing Forge Item Registry...");
                ITEMS.register(bus);
                CREATIVE_TABS.register(bus);
        }
}
