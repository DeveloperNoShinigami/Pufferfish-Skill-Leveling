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
 * Each tier targets a specific slot:
 * - Tome of Cleansing (I): Slot 0 (first skill)
 * - Tome of Cleansing II: Slot 1 (second skill)
 * - Tome of Cleansing III: Slot 2 (third skill)
 */
public class TomeOfCleansingItem extends Item {

    private final int targetSlot;

    public TomeOfCleansingItem(Settings settings, int targetSlot) {
        super(settings);
        this.targetSlot = targetSlot;
    }

    /**
     * Gets the slot index this tome targets (0, 1, or 2).
     */
    public int getTargetSlot() {
        return targetSlot;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        // Show which slot this tome targets (display as 1-indexed for players)
        int displaySlot = targetSlot + 1;
        tooltip.add(Text.translatable("item.puffish_skill_leveling.tome_of_cleansing.slot", displaySlot)
                .formatted(Formatting.GOLD));

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
