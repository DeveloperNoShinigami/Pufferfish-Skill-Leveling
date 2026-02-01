package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.item.SigilOfImbuementItem;
import net.bluelotuscoding.skillleveling.item.SkillTomeItem;
import net.bluelotuscoding.skillleveling.item.TomeItem;
import net.bluelotuscoding.skillleveling.item.TomeOfCleansingItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import java.util.function.Consumer;

/**
 * Common item definitions for Tome items.
 * Platform-specific registration is handled in FabricMain/ForgeMain.
 */
public class ModItems {

        public static final Identifier TOME_PROGRESSION_ID = SkillLevelingMod.createIdentifier("tome_of_progression");
        public static final Identifier TOME_CLEAR_MIND_ID = SkillLevelingMod.createIdentifier("tome_of_clear_mind");
        public static final Identifier TOME_GREATER_CLEAR_MIND_ID = SkillLevelingMod
                        .createIdentifier("tome_of_greater_clear_mind");
        public static final Identifier SKILL_TOME_ID = SkillLevelingMod.createIdentifier("skill_tome");
        public static final Identifier SIGIL_OF_IMBUEMENT_ID = SkillLevelingMod.createIdentifier("sigil_of_imbuement");
        public static final Identifier TOME_OF_CLEANSING_ID = SkillLevelingMod.createIdentifier("tome_of_cleansing");
        public static final Identifier TOME_OF_CLEANSING_2_ID = SkillLevelingMod
                        .createIdentifier("tome_of_cleansing_2");
        public static final Identifier TOME_OF_CLEANSING_3_ID = SkillLevelingMod
                        .createIdentifier("tome_of_cleansing_3");
        public static final Identifier BLANK_TOME_ID = SkillLevelingMod.createIdentifier("blank_tome");

        // Item instances - these will be populated during platform registration
        public static TomeItem TOME_OF_PROGRESSION;
        public static TomeItem TOME_OF_CLEAR_MIND;
        public static TomeItem TOME_OF_GREATER_CLEAR_MIND;
        public static SkillTomeItem SKILL_TOME;
        public static SigilOfImbuementItem SIGIL_OF_IMBUEMENT;
        public static TomeOfCleansingItem TOME_OF_CLEANSING;
        public static TomeOfCleansingItem TOME_OF_CLEANSING_2;
        public static TomeOfCleansingItem TOME_OF_CLEANSING_3;
        public static Item BLANK_TOME;
        public static Item SKILL_SCRIBE_TABLE_ITEM;

        /**
         * Create the Tome of Progression item.
         */
        public static TomeItem createTomeOfProgression() {
                return new TomeItem(
                                new Item.Settings()
                                                .maxCount(16)
                                                .rarity(Rarity.UNCOMMON),
                                TomeItem.TomeType.PROGRESSION);
        }

        /**
         * Create the Tome of Clear Mind item.
         */
        public static TomeItem createTomeOfClearMind() {
                return new TomeItem(
                                new Item.Settings()
                                                .maxCount(16)
                                                .rarity(Rarity.RARE),
                                TomeItem.TomeType.CLEAR_MIND);
        }

        /**
         * Create the Tome of Greater Clear Mind item.
         */
        public static TomeItem createTomeOfGreaterClearMind() {
                return new TomeItem(
                                new Item.Settings()
                                                .maxCount(8)
                                                .rarity(Rarity.EPIC),
                                TomeItem.TomeType.GREATER_CLEAR_MIND);
        }

        /**
         * Create the Skill Tome item.
         */
        public static SkillTomeItem createSkillTome() {
                return new SkillTomeItem(
                                new Item.Settings()
                                                .maxCount(16)
                                                .rarity(Rarity.RARE));
        }

        /**
         * Create the Sigil of Imbuement item.
         */
        public static SigilOfImbuementItem createSigilOfImbuement() {
                return new SigilOfImbuementItem(
                                new Item.Settings()
                                                .maxCount(16)
                                                .rarity(Rarity.EPIC));
        }

        /**
         * Create the Tome of Cleansing item (targets slot 0).
         */
        public static TomeOfCleansingItem createTomeOfCleansing() {
                return new TomeOfCleansingItem(
                                new Item.Settings()
                                                .maxCount(16)
                                                .rarity(Rarity.EPIC),
                                0); // Target slot 0
        }

        /**
         * Create the Tome of Cleansing II item (targets slot 1).
         */
        public static TomeOfCleansingItem createTomeOfCleansing2() {
                return new TomeOfCleansingItem(
                                new Item.Settings()
                                                .maxCount(16)
                                                .rarity(Rarity.EPIC),
                                1); // Target slot 1
        }

        /**
         * Create the Tome of Cleansing III item (targets slot 2).
         */
        public static TomeOfCleansingItem createTomeOfCleansing3() {
                return new TomeOfCleansingItem(
                                new Item.Settings()
                                                .maxCount(16)
                                                .rarity(Rarity.EPIC),
                                2); // Target slot 2
        }

        /**
         * Create the Blank Tome item.
         */
        public static Item createBlankTome() {
                return new Item(new Item.Settings().maxCount(16).rarity(Rarity.COMMON));
        }

        /**
         * Populates the Base Tomes creative tab.
         */
        public static void fillBaseTomesTab(Consumer<ItemStack> entries) {
                if (BLANK_TOME != null) {
                        entries.accept(new ItemStack(BLANK_TOME));
                }
                if (TOME_OF_PROGRESSION != null) {
                        entries.accept(new ItemStack(TOME_OF_PROGRESSION));
                }
                if (TOME_OF_CLEAR_MIND != null) {
                        entries.accept(new ItemStack(TOME_OF_CLEAR_MIND));
                }
                if (TOME_OF_GREATER_CLEAR_MIND != null) {
                        entries.accept(new ItemStack(TOME_OF_GREATER_CLEAR_MIND));
                }
                if (TOME_OF_CLEANSING != null) {
                        entries.accept(new ItemStack(TOME_OF_CLEANSING));
                }
                if (TOME_OF_CLEANSING_2 != null) {
                        entries.accept(new ItemStack(TOME_OF_CLEANSING_2));
                }
                if (TOME_OF_CLEANSING_3 != null) {
                        entries.accept(new ItemStack(TOME_OF_CLEANSING_3));
                }
                if (SIGIL_OF_IMBUEMENT != null) {
                        entries.accept(new ItemStack(SIGIL_OF_IMBUEMENT));
                }
                if (SKILL_SCRIBE_TABLE_ITEM != null) {
                        entries.accept(new ItemStack(SKILL_SCRIBE_TABLE_ITEM));
                }
        }

        /**
         * Populates the Skill Tomes creative tab.
         */
        public static void fillSkillTomesTab(Consumer<ItemStack> entries) {
                var entriesMap = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.getAllEntries();
                if (SKILL_TOME != null) {
                        for (var entry : entriesMap.entrySet()) {
                                String skillId = entry.getKey();
                                var config = entry.getValue();
                                // Only add tomes for skills that have explicit loot_mode and category_id
                                if (config.lootMode != null && config.categoryId != null) {
                                        // Generate tome for each level from 1 to maxLevels
                                        for (int level = 1; level <= config.maxLevels; level++) {
                                                entries.accept(SkillTomeItem.createSkillTome(SKILL_TOME,
                                                                config.categoryId,
                                                                skillId, config.lootMode, level));
                                        }
                                }
                        }
                }
        }
}
