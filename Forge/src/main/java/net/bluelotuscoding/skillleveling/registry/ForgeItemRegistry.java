package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.item.SigilOfImbuementItem;
import net.bluelotuscoding.skillleveling.item.SkillCharmItem;
import net.bluelotuscoding.skillleveling.item.SkillTomeItem;
import net.bluelotuscoding.skillleveling.item.TomeItem;
import net.bluelotuscoding.skillleveling.item.TomeOfCleansingItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ForgeItemRegistry {
        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
                        SkillLevelingMod.MOD_ID);

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
        public static final RegistryObject<Item> BLANK_TOME = ITEMS
                        .register("blank_tome", ModItems::createBlankTome);
        public static final RegistryObject<SkillCharmItem> SKILL_CHARM = ITEMS
                        .register("skill_charm", ModItems::createSkillCharm);

        // Block Items
        public static final RegistryObject<Item> SKILL_SCRIBE_TABLE_ITEM = ITEMS.register("skill_scribe_table",
                        () -> new BlockItem(ForgeBlockRegistry.SKILL_SCRIBE_TABLE.get(),
                                        new Item.Settings().rarity(Rarity.RARE)));

        public static void register(IEventBus bus) {
                SkillLevelingMod.getInstance().getLogger().info("Initializing Forge Item Registry...");
                ITEMS.register(bus);
        }
}
