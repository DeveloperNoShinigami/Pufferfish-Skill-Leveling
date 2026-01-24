package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.item.TomeItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

/**
 * Common item definitions for Tome items.
 * Platform-specific registration is handled in FabricMain/ForgeMain.
 */
public class ModItems {

    public static final Identifier TOME_PROGRESSION_ID = SkillLevelingMod.createIdentifier("tome_of_progression");
    public static final Identifier TOME_CLEAR_MIND_ID = SkillLevelingMod.createIdentifier("tome_of_clear_mind");
    public static final Identifier TOME_GREATER_CLEAR_MIND_ID = SkillLevelingMod
            .createIdentifier("tome_of_greater_clear_mind");

    // Item instances - these will be populated during platform registration
    public static TomeItem TOME_OF_PROGRESSION;
    public static TomeItem TOME_OF_CLEAR_MIND;
    public static TomeItem TOME_OF_GREATER_CLEAR_MIND;

    /**
     * Create the Tome of Progression item.
     * +1 category level → grants 1 skill point.
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
     * -1 skill level on selected skill → refund points.
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
     * Reset selected skill to 0 → refund all points for that skill.
     */
    public static TomeItem createTomeOfGreaterClearMind() {
        return new TomeItem(
                new Item.Settings()
                        .maxCount(8)
                        .rarity(Rarity.EPIC),
                TomeItem.TomeType.GREATER_CLEAR_MIND);
    }
}
