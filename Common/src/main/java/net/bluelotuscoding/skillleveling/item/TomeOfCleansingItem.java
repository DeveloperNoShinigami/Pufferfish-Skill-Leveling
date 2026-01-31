package net.bluelotuscoding.skillleveling.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tome of Cleansing - Extracts an imbued skill from equipment.
 * 
 * When used in an anvil with imbued equipment:
 * - Player selects which skill to extract
 * - The skill is removed from the equipment
 * - A Skill Tome with the extracted skill is returned
 * - The skill slot remains open on the equipment
 */
public class TomeOfCleansingItem extends Item {

    public TomeOfCleansingItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.puffish_skill_leveling.tome_of_cleansing.desc1")
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.puffish_skill_leveling.tome_of_cleansing.desc2")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
