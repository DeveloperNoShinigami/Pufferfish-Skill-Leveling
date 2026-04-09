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
            for (ImbuedSkillHelper.ImbuedSkill skill : skills) {
                String displayName = toTitleCase(skill.skillId);
                tooltip.add(Text.literal(displayName + " +" + skill.level).formatted(Formatting.GOLD));

                String description = resolveDescription(skill.skillId, skill.level);
                if (description != null && !description.isEmpty()) {
                    tooltip.add(Text.literal(description).formatted(Formatting.BLUE));
                }
            }
        }

        tooltip.add(Text.translatable("item.puffish_skill_leveling.skill_charm.hint")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    private static String resolveDescription(String skillId, int level) {
        String description = net.bluelotuscoding.skillleveling.client.ClientDescriptionStorage
                .getDescriptionSingle(skillId, level);
        if (description != null) {
            return description;
        }

        for (String key : net.bluelotuscoding.skillleveling.client.ClientDescriptionStorage.getAllKeys()) {
            if (isFuzzySkillMatch(key, skillId)) {
                description = net.bluelotuscoding.skillleveling.client.ClientDescriptionStorage
                        .getDescriptionSingle(key, level);
                if (description != null) {
                    return description;
                }
            }
        }

        return null;
    }

    private static boolean isFuzzySkillMatch(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.equals(right)) {
            return true;
        }

        net.minecraft.util.Identifier leftId = net.minecraft.util.Identifier.tryParse(left);
        net.minecraft.util.Identifier rightId = net.minecraft.util.Identifier.tryParse(right);
        if (leftId != null && rightId != null) {
            return leftId.getPath().equals(rightId.getPath());
        }

        String leftPath = left.contains(":") ? left.substring(left.indexOf(':') + 1) : left;
        String rightPath = right.contains(":") ? right.substring(right.indexOf(':') + 1) : right;
        return leftPath.equals(rightPath);
    }

    private static String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String normalized = text.contains(":") ? text.substring(text.indexOf(':') + 1) : text;
        String[] parts = normalized.split("_");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }

        return result.toString();
    }
}
