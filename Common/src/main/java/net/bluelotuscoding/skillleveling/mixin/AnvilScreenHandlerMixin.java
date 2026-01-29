package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.item.SkillTomeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to support imbuing skills onto equipment via an anvil.
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ScreenHandler {

    @Shadow
    @Final
    private Property levelCost;

    @Shadow
    private int repairItemUsage;

    @Unique
    private boolean skillLeveling$isSkillImbue = false;

    protected AnvilScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(method = "updateResult", at = @At("RETURN"))
    private void onUpdateResult(CallbackInfo ci) {
        // Use slots accessor which is available on ScreenHandler
        ItemStack slot1 = this.slots.get(0).getStack();
        ItemStack slot2 = this.slots.get(1).getStack();

        skillLeveling$isSkillImbue = false;

        if (slot1.isEmpty() || slot2.isEmpty()) {
            return;
        }

        // Check if slot 2 is a Skill Tome
        if (slot2.getItem() instanceof SkillTomeItem) {
            String lootMode = SkillTomeItem.getLootMode(slot2);

            // Only allow imbuing if the tome's loot mode is "imbue_only" or "both"
            if (SkillTomeItem.LOOT_MODE_IMBUE_ONLY.equals(lootMode) || SkillTomeItem.LOOT_MODE_BOTH.equals(lootMode)) {
                String categoryId = SkillTomeItem.getCategoryId(slot2);
                String skillId = SkillTomeItem.getSkillId(slot2);

                if (categoryId != null && skillId != null) {
                    // Create copy of slot 1 with imbue NBT
                    ItemStack result = slot1.copy();

                    NbtCompound nbt = result.getOrCreateNbt();
                    NbtCompound skillLevelingNbt = new NbtCompound();
                    // Normalize category ID to include a namespace so server and client
                    // comparisons match (e.g., "skillleveling_template:example").
                    String normalizedCategory = categoryId.contains(":") ? categoryId
                            : "skillleveling_template:" + categoryId;
                    skillLevelingNbt.putString("CategoryId", normalizedCategory);
                    skillLevelingNbt.putString("SkillId", skillId);
                    nbt.put("SkillLevelingImbued", skillLevelingNbt);

                    // Update output slot (slot index 2 in anvil)
                    this.slots.get(2).setStack(result);

                    // Set experience cost to 0 (free imbuing)
                    this.levelCost.set(0);
                    // Consume only 1 tome instead of the whole stack
                    this.repairItemUsage = 1;
                    
                    skillLeveling$isSkillImbue = true;

                    // Force sync the changes
                    this.sendContentUpdates();
                }
            }
        } else {
            this.skillLeveling$isSkillImbue = false;
        }
    }

    @Inject(method = "canTakeOutput", at = @At("HEAD"), cancellable = true)
    private void onCanTakeOutput(PlayerEntity player, boolean present, CallbackInfoReturnable<Boolean> cir) {
        // Always allow taking output if this is our skill imbue
        // Vanilla requires levelCost > 0, but we set it to 0, so we must override this
        // check
        if (skillLeveling$isSkillImbue && present) {
            cir.setReturnValue(true);
        }
    }
}
