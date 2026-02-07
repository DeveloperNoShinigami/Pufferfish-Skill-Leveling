package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.util.ImbuedSkillHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mixin to add skill information to tooltips for items imbued with skills.
 * Supports the new multi-skill slot system.
 */
@Mixin(ItemStack.class)
public abstract class ItemTooltipMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void onGetTooltip(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        NbtCompound nbt = stack.getNbt();

        if (nbt == null || !nbt.contains("SkillLevelingImbued")) {
            return;
        }

        List<Text> tooltip = cir.getReturnValue();

        // Migrate old format if needed (for display purposes)
        ImbuedSkillHelper.migrateOldFormat(stack);

        // Get slot count and skills
        int slots = ImbuedSkillHelper.getSlotCount(stack);
        List<ImbuedSkillHelper.ImbuedSkill> skills = ImbuedSkillHelper.getSkills(stack);

        // Add spacing
        tooltip.add(Text.literal(" "));

        // Show slot info if slots exist
        if (slots > 0) {
            int usedSlots = skills.size();
            tooltip.add(Text.literal("◈ Equipment Slots: ")
                    .formatted(Formatting.DARK_PURPLE)
                    .append(Text.literal(usedSlots + "/" + slots)
                            .formatted(usedSlots >= slots ? Formatting.RED : Formatting.GREEN)));
        }

        // Show each imbued skill
        for (ImbuedSkillHelper.ImbuedSkill skill : skills) {
            String displayName = convertToTitleCase(skill.skillId);
            tooltip.add(Text.literal("  ✦ " + displayName + " ")
                    .formatted(Formatting.GOLD)
                    .append(Text.literal("+" + skill.level)
                            .formatted(Formatting.AQUA)));
        }
    }

    private String convertToTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        String[] parts = text.split("_");

        for (String part : parts) {
            if (!part.isEmpty()) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }
}
