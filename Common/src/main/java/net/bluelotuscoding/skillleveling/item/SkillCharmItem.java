package net.bluelotuscoding.skillleveling.item;

import net.bluelotuscoding.skillleveling.util.ImbuedSkillHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Skill Charm - An accessory that provides skill bonuses when equipped in a
 * Curio slot.
 * Uses the standard ImbuedSkillHelper NBT format.
 */
public class SkillCharmItem extends Item {

    public SkillCharmItem(Settings settings) {
        super(settings);
    }

    /**
     * Creates a Skill Charm ItemStack for the given skill and level.
     */
    public static ItemStack createSkillCharm(Item charmItem, String categoryId, String skillId, int level) {
        ItemStack stack = new ItemStack(charmItem);
        // Charms have 1 slot by default
        ImbuedSkillHelper.setSlotCount(stack, 1);
        ImbuedSkillHelper.addSkill(stack, categoryId, skillId, level);
        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        List<ImbuedSkillHelper.ImbuedSkill> skills = ImbuedSkillHelper.getSkills(stack);
        if (skills.isEmpty()) {
            tooltip.add(Text.translatable("item.puffish_skill_leveling.skill_charm.unconfigured")
                    .formatted(Formatting.RED, Formatting.ITALIC));
        } else {
            // Bonuses are now handled by ItemTooltipMixin for all imbued items
            // preserving consistency across Gear and Curios.
        }

        tooltip.add(Text.translatable("item.puffish_skill_leveling.skill_charm.hint")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
