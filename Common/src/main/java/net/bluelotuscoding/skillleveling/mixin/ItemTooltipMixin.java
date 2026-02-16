package net.bluelotuscoding.skillleveling.mixin;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.entity.player.PlayerEntity;
import java.util.ArrayList;
import java.util.Collections;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mixin to add skill information to tooltips for items imbued with skills.
 */
@Mixin(ItemStack.class)
public abstract class ItemTooltipMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void onGetTooltip(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        try {
            ItemStack stack = (ItemStack) (Object) this;
            NbtCompound nbt = stack.getNbt();

            if (nbt != null && nbt.contains("SkillLevelingImbued")) {
                NbtCompound imbuedNbt = nbt.getCompound("SkillLevelingImbued");
                String skillId = imbuedNbt.getString("SkillId");

                if (skillId != null && !skillId.isEmpty()) {
                    List<Text> original = cir.getReturnValue();
                    List<Text> tooltip = new ArrayList<>(original != null ? original : Collections.emptyList());

                    // Convert snake_case to Title Case
                    String displayName = convertToTitleCase(skillId);

                    tooltip.add(Text.literal(" "));
                    tooltip.add(Text.literal("Imbued Skill:").formatted(Formatting.GRAY));
                    tooltip.add(Text.literal(" " + displayName).formatted(Formatting.GOLD));

                    // Fetch real description for level 1 from client storage
                    String description = net.bluelotuscoding.skillleveling.client.ClientDescriptionStorage
                            .getDescriptionSingle(skillId, 1);
                    if (description != null) {
                        tooltip.add(Text.literal(" " + description).formatted(Formatting.BLUE));
                    } else {
                        tooltip.add(Text.literal(" +1 Level (When Equipped)").formatted(Formatting.BLUE));
                    }

                    cir.setReturnValue(tooltip);
                }
            }
        } catch (Exception e) {
            // Keep original tooltip on error
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
