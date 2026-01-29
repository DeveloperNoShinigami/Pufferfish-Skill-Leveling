package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.item.SkillTomeItem;
import net.bluelotuscoding.skillleveling.item.TomeItem;
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

        // Item instances - these will be populated during platform registration
        public static TomeItem TOME_OF_PROGRESSION;
        public static TomeItem TOME_OF_CLEAR_MIND;
        public static TomeItem TOME_OF_GREATER_CLEAR_MIND;
        public static SkillTomeItem SKILL_TOME;

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
         * Populates the Base Tomes creative tab.
         */
        public static void fillBaseTomesTab(Consumer<ItemStack> entries) {
                if (TOME_OF_PROGRESSION != null) {
                        entries.accept(new ItemStack(TOME_OF_PROGRESSION));
                }
                if (TOME_OF_CLEAR_MIND != null) {
                        entries.accept(new ItemStack(TOME_OF_CLEAR_MIND));
                }
                if (TOME_OF_GREATER_CLEAR_MIND != null) {
                        entries.accept(new ItemStack(TOME_OF_GREATER_CLEAR_MIND));
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
                                        entries.accept(SkillTomeItem.createSkillTome(SKILL_TOME, config.categoryId,
                                                        skillId, config.lootMode));
                                }
                        }
                }
        }
}
