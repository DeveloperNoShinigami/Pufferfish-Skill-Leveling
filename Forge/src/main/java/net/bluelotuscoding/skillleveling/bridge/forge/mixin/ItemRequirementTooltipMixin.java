package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.bluelotuscoding.skillleveling.bridge.config.ItemRequirementsManager;
import net.bluelotuscoding.skillleveling.client.ItemRestrictionKeybind;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

@Mixin(ItemStack.class)
public abstract class ItemRequirementTooltipMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void appendItemRequirements(@Nullable PlayerEntity player, TooltipContext context,
            CallbackInfoReturnable<List<Text>> cir) {
        if (player == null) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;
        if (stack.isEmpty()) {
            return;
        }

        String itemId = Registries.ITEM.getId(stack.getItem()).toString();

        // Skip if this item doesn't have requirements defined
        net.bluelotuscoding.skillleveling.bridge.config.ItemRequirementDef def = ItemRequirementsManager
                .getRequirements(itemId);
        if (def == null) {
            return;
        }

        // Skip if tooltips are explicitly disabled for this definition
        if (def.tooltip != null && !def.tooltip) {
            return;
        }

        List<String> failures = ItemRequirementsManager.checkRequirements(player, itemId,
                ItemRequirementsManager.TargetType.ITEM);

        // If all requirements are met, show nothing (clean item)
        if (failures.isEmpty()) {
            return;
        }

        List<Text> tooltips = cir.getReturnValue();
        tooltips.add(Text.literal(" "));

        // Check keybind state
        if (ItemRestrictionKeybind.isShowingRestrictions()) {
            // Show full restriction list
            tooltips.add(Text.literal("\u00A7c\u00A7l\u2716 Requirements Not Met:"));
            for (String failure : failures) {
                tooltips.add(Text.literal("\u00A7c  \u2022 " + failure));
            }
        } else {
            // Show hint to toggle
            String keyName = ItemRestrictionKeybind.getKeyName();
            tooltips.add(Text.literal("\u00A7e[Hold " + keyName + " to view restrictions]"));
        }
    }
}
