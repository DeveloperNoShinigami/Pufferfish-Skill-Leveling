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

        // Check if both slots are Skill Tomes (Combining)
        if (slot1.getItem() instanceof SkillTomeItem && slot2.getItem() instanceof SkillTomeItem) {
            String skillId1 = SkillTomeItem.getSkillId(slot1);
            String categoryId1 = SkillTomeItem.getCategoryId(slot1);
            int level1 = SkillTomeItem.getLevel(slot1);

            String skillId2 = SkillTomeItem.getSkillId(slot2);
            String categoryId2 = SkillTomeItem.getCategoryId(slot2);
            int level2 = SkillTomeItem.getLevel(slot2);

            if (skillId1 != null && skillId1.equals(skillId2) &&
                    categoryId1 != null && categoryId1.equals(categoryId2) &&
                    level1 == level2) {

                net.bluelotuscoding.skillleveling.manager.SkillLevelingManager manager = net.bluelotuscoding.skillleveling.SkillLevelingMod
                        .getInstance()
                        .getSkillLevelingManager();

                if (manager != null) {
                    net.minecraft.util.Identifier catId = categoryId1.contains(":")
                            ? new net.minecraft.util.Identifier(categoryId1)
                            : new net.minecraft.util.Identifier("skillleveling_template", categoryId1);
                    int maxLevel = manager.getMaxLevel(catId, skillId1);

                    if (level1 < maxLevel) {
                        String lootMode = SkillTomeItem.getLootMode(slot1);
                        ItemStack result = SkillTomeItem.createSkillTome(slot1.getItem(), categoryId1, skillId1,
                                lootMode, level1 + 1);

                        this.slots.get(2).setStack(result);

                        // Experience cost: configured enchantment_levels * current level
                        int enchantmentCost = 0;
                        var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId1);
                        if (config != null) {
                            enchantmentCost = config.enchantmentLevels * level1;
                        }

                        this.levelCost.set(enchantmentCost);
                        this.repairItemUsage = 1;

                        skillLeveling$isSkillImbue = true;
                        this.sendContentUpdates();
                        return;
                    }
                }
            }
        }

        // Check if slot 2 is a Skill Tome (Imbuing onto equipment)
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
                    int level = SkillTomeItem.getLevel(slot2);
                    skillLevelingNbt.putString("CategoryId", normalizedCategory);
                    skillLevelingNbt.putString("SkillId", skillId);
                    skillLevelingNbt.putInt("Level", level);
                    nbt.put("SkillLevelingImbued", skillLevelingNbt);

                    // Update output slot (slot index 2 in anvil)
                    this.slots.get(2).setStack(result);

                    // Set experience cost to 0 (free imbuing onto gear)
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
        // Explicitly override taking output for skill actions
        if (skillLeveling$isSkillImbue && present) {
            int cost = this.levelCost.get();

            // 1. Creative mode always allowed
            if (player.getAbilities().creativeMode) {
                cir.setReturnValue(true);
                return;
            }

            // 2. Paid actions (combining tomes) require sufficient levels
            if (cost > 0) {
                if (player.experienceLevel >= cost) {
                    cir.setReturnValue(true);
                } else {
                    // Force gate if insufficient levels
                    cir.setReturnValue(false);
                }
            } else {
                // 3. Free actions (imbuing equipment) are always allowed
                cir.setReturnValue(true);
            }
        }
    }
}
