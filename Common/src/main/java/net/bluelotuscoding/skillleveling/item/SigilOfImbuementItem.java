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
 * Sigil of Imbuement - Opens a skill slot on equipment when used in an anvil.
 * 
 * Players can open up to 3 skill slots on any piece of equipment.
 * Each slot can then hold one imbued skill.
 */
public class SigilOfImbuementItem extends Item {

    public SigilOfImbuementItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.puffish_skill_leveling.sigil_of_imbuement.desc1")
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.puffish_skill_leveling.sigil_of_imbuement.desc2")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
