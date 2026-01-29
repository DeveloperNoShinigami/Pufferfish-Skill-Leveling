package net.bluelotuscoding.skillleveling.mixin;

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
 */
@Mixin(ItemStack.class)
public abstract class ItemTooltipMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void onGetTooltip(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        NbtCompound nbt = stack.getNbt();

        if (nbt != null && nbt.contains("SkillLevelingImbued")) {
            NbtCompound imbuedNbt = nbt.getCompound("SkillLevelingImbued");
            String skillId = imbuedNbt.getString("SkillId");
            // CategoryId is also stored but usually not needed for basic display

            if (skillId != null && !skillId.isEmpty()) {
                List<Text> tooltip = cir.getReturnValue();

                // Convert snake_case to Title Case
                String displayName = convertToTitleCase(skillId);
                int level = imbuedNbt.contains("Level") ? imbuedNbt.getInt("Level") : 1;

                tooltip.add(Text.literal(" "));
                // CONCISE TOOLTIP: [ Skill Name ] +N level(s)
                tooltip.add(Text.literal("[ " + displayName + " ] ")
                        .formatted(Formatting.GOLD)
                        .append(Text.literal("+" + level + (level == 1 ? " level" : " levels"))
                                .formatted(Formatting.AQUA)));
            }
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
