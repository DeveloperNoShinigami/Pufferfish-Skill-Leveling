package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.item.SkillTomeItem;
import net.bluelotuscoding.skillleveling.item.SigilOfImbuementItem;
import net.bluelotuscoding.skillleveling.item.TomeOfCleansingItem;
import net.bluelotuscoding.skillleveling.util.ImbuedSkillHelper;
import net.bluelotuscoding.skillleveling.registry.ModItems;
import net.minecraft.item.ItemStack;
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

import java.util.List;

/**
 * Mixin to support multi-skill imbuing onto equipment via an anvil.
 * 
 * Supported operations:
 * 1. Sigil + Gear → Open skill slot (max 3)
 * 2. Skill Tome + Slotted Gear → Imbue skill into empty slot
 * 3. Skill Tome + Gear with same skill → Upgrade skill level
 * 4. Tome of Cleansing + Imbued Gear → Extract skill (returns as tome)
 * 5. Skill Tome + Skill Tome → Combine tomes (existing logic)
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

    @Unique
    private boolean skillLeveling$isSigilSlotOpen = false;

    @Unique
    private boolean skillLeveling$isCleansing = false;

    @Unique
    private ItemStack skillLeveling$cleansingRefundTome = ItemStack.EMPTY;

    protected AnvilScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(method = "updateResult", at = @At("RETURN"))
    private void onUpdateResult(CallbackInfo ci) {
        ItemStack slot1 = this.slots.get(0).getStack();
        ItemStack slot2 = this.slots.get(1).getStack();

        // Reset flags
        skillLeveling$isSkillImbue = false;
        skillLeveling$isSigilSlotOpen = false;
        skillLeveling$isCleansing = false;
        skillLeveling$cleansingRefundTome = ItemStack.EMPTY;

        if (slot1.isEmpty() || slot2.isEmpty()) {
            return;
        }

        // Migrate old NBT format if needed
        ImbuedSkillHelper.migrateOldFormat(slot1);

        // === 1. Sigil of Imbuement + Gear → Open Slot ===
        if (slot2.getItem() instanceof SigilOfImbuementItem) {
            if (ImbuedSkillHelper.canOpenMoreSlots(slot1)) {
                ItemStack result = slot1.copy();
                int currentSlots = ImbuedSkillHelper.getSlotCount(result);
                ImbuedSkillHelper.setSlotCount(result, currentSlots + 1);

                this.slots.get(2).setStack(result);

                // Cost: configurable, default to 0 (free)
                int slotCost = getSlotOpeningCost(slot1);
                this.levelCost.set(slotCost);
                this.repairItemUsage = 1;
                skillLeveling$isSigilSlotOpen = true;
                this.sendContentUpdates();
                return;
            }
        }

        // === 2. Tome of Cleansing + Imbued Gear → Extract Skill (slot-targeted) ===
        if (slot2.getItem() instanceof TomeOfCleansingItem cleansingItem) {
            int targetSlot = cleansingItem.getTargetSlot();
            ImbuedSkillHelper.ImbuedSkill skillToExtract = ImbuedSkillHelper.getSkillByIndex(slot1, targetSlot);

            if (skillToExtract != null) {
                ItemStack result = slot1.copy();
                ImbuedSkillHelper.removeSkillByIndex(result, targetSlot);

                // Create the refund tome
                var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillToExtract.skillId);
                if (config == null || config.lootMode == null)
                    return; // If config or lootMode is null, we cannot determine the refund tome's loot
                            // mode.
                String lootMode = config.lootMode;

                skillLeveling$cleansingRefundTome = SkillTomeItem.createSkillTome(
                        ModItems.SKILL_TOME,
                        skillToExtract.categoryId,
                        skillToExtract.skillId,
                        lootMode,
                        skillToExtract.level);

                this.slots.get(2).setStack(result);

                // Cost: configurable, default to 0 (free)
                int cleansingCost = getCleansingCost(skillToExtract.skillId, skillToExtract.level);
                this.levelCost.set(cleansingCost);
                this.repairItemUsage = 1;
                skillLeveling$isCleansing = true;
                skillLeveling$isSkillImbue = true;
                this.sendContentUpdates();
                return;
            }
        }

        // === 3. Skill Tome + Skill Tome → Combine (existing logic) ===
        if (slot1.getItem() instanceof SkillTomeItem && slot2.getItem() instanceof SkillTomeItem) {
            String skillId1 = SkillTomeItem.getSkillId(slot1);
            String categoryId1 = SkillTomeItem.getCategoryId(slot1);
            int level1 = SkillTomeItem.getLevel(slot1);

            String skillId2 = SkillTomeItem.getSkillId(slot2);
            String categoryId2 = SkillTomeItem.getCategoryId(slot2);
            int level2 = SkillTomeItem.getLevel(slot2);

            boolean skillMatch = skillId1 != null && skillId1.equals(skillId2);
            boolean categoryMatch = categoryIdsMatch(categoryId1, categoryId2);

            if (skillMatch && categoryMatch && level1 == level2) {
                net.bluelotuscoding.skillleveling.manager.SkillLevelingManager manager = net.bluelotuscoding.skillleveling.SkillLevelingMod
                        .getInstance()
                        .getSkillLevelingManager();

                if (manager != null) {
                    // Normalize category ID for config lookup and level check
                    net.minecraft.util.Identifier normalizedCatId = manager
                            .normalizeCategoryId(new net.minecraft.util.Identifier(categoryId1));
                    int maxLevel = manager.getMaxLevel(normalizedCatId, skillId1);

                    if (level1 < maxLevel) {
                        String lootMode = SkillTomeItem.getLootMode(slot1);
                        ItemStack result = SkillTomeItem.createSkillTome(slot1.getItem(), categoryId1, skillId1,
                                lootMode, level1 + 1);

                        this.slots.get(2).setStack(result);

                        int enchantmentCost = 0;
                        var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId1);
                        if (config == null || config.lootMode == null)
                            return;
                        if (config != null && config.enchantmentCost != null) {
                            enchantmentCost = config.enchantmentCost.getCost(level1);
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

        // === 4. Skill Tome + Gear → Imbue or Upgrade ===
        if (slot2.getItem() instanceof SkillTomeItem) {
            String lootMode = SkillTomeItem.getLootMode(slot2);

            // Only allow imbuing if the tome's loot mode is "imbue_only" or "both"
            if (SkillTomeItem.LOOT_MODE_IMBUE_ONLY.equals(lootMode) || SkillTomeItem.LOOT_MODE_BOTH.equals(lootMode)) {
                String categoryId = SkillTomeItem.getCategoryId(slot2);
                String skillId = SkillTomeItem.getSkillId(slot2);
                int tomeLevel = SkillTomeItem.getLevel(slot2);

                if (categoryId != null && skillId != null) {
                    ItemStack result = slot1.copy();
                    ImbuedSkillHelper.migrateOldFormat(result);

                    // Check if gear already has this skill (upgrade scenario)
                    int existingLevel = ImbuedSkillHelper.getSkillLevel(result, skillId);

                    if (existingLevel > 0 && existingLevel == tomeLevel) {
                        // === Upgrade scenario: same skill, same level → level + 1 ===
                        var manager = net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance()
                                .getSkillLevelingManager();
                        if (manager != null) {
                            net.minecraft.util.Identifier catId = manager
                                    .normalizeCategoryId(new net.minecraft.util.Identifier(categoryId));
                            int maxLevel = manager.getMaxLevel(catId, skillId);

                            if (existingLevel < maxLevel) {
                                var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
                                if (config == null || config.lootMode == null)
                                    return;
                                ImbuedSkillHelper.upgradeSkill(result, skillId);

                                this.slots.get(2).setStack(result);

                                int upgradeCost = 0;
                                if (config != null && config.imbuementCost != null) {
                                    upgradeCost = config.imbuementCost.getCost(existingLevel + 1);
                                }

                                this.levelCost.set(upgradeCost);
                                this.repairItemUsage = 1;
                                skillLeveling$isSkillImbue = true;
                                this.sendContentUpdates();
                                return;
                            }
                        }
                    } else if (existingLevel == 0 && ImbuedSkillHelper.hasEmptySlot(result)) {
                        // === Imbue scenario: new skill into empty slot ===
                        ImbuedSkillHelper.addSkill(result, categoryId, skillId, tomeLevel);

                        this.slots.get(2).setStack(result);

                        int imbuementCost = 0;
                        var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
                        if (config == null || config.lootMode == null)
                            return;
                        if (config != null && config.imbuementCost != null) {
                            imbuementCost = config.imbuementCost.getCost(tomeLevel);
                        }

                        this.levelCost.set(imbuementCost);
                        this.repairItemUsage = 1;
                        skillLeveling$isSkillImbue = true;
                        this.sendContentUpdates();
                        return;
                    }
                    // If no empty slots or skill already at different level, don't do imbuing
                }
            }
        }
    }

    /**
     * Get the XP cost for opening a slot. Defaults to 0 (free).
     * Uses the cost defined in any skill of the same category.
     */
    @Unique
    private int getSlotOpeningCost(ItemStack stack) {
        int nextSlot = ImbuedSkillHelper.getSlotCount(stack) + 1;
        List<ImbuedSkillHelper.ImbuedSkill> skills = ImbuedSkillHelper.getSkills(stack);

        String categoryId = null;
        if (!skills.isEmpty()) {
            categoryId = skills.get(0).categoryId;
        }

        for (var entry : net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.getAllEntries().values()) {
            if (categoryId == null || categoryId.equals(entry.categoryId)) {
                if (entry.slotOpeningCost != net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.EnchantmentCostConfig.FREE) {
                    return entry.slotOpeningCost.getCost(nextSlot);
                }
            }
        }
        return 0;
    }

    /**
     * Get the XP cost for cleansing a skill. Defaults to 0 (free).
     */
    @Unique
    private int getCleansingCost(String skillId, int level) {
        var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
        if (config != null && config.cleansingCost != null) {
            return config.cleansingCost.getCost(level);
        }
        return 0;
    }

    /**
     * Helper to match category IDs with namespace flexibility.
     */
    @Unique
    private boolean categoryIdsMatch(String categoryId1, String categoryId2) {
        if (categoryId1 == null || categoryId2 == null) {
            return false;
        }
        if (categoryId1.equals(categoryId2)) {
            return true;
        }
        // Fallback: Path-only match for namespace flexibility
        net.minecraft.util.Identifier id1 = net.minecraft.util.Identifier.tryParse(categoryId1);
        net.minecraft.util.Identifier id2 = net.minecraft.util.Identifier.tryParse(categoryId2);
        return id1 != null && id2 != null && id1.getPath().equals(id2.getPath());
    }

    /**
     * Injects into taking the result to handle custom stack reduction
     * and give back the cleansing refund tome.
     */
    @Inject(method = "onTakeOutput", at = @At("HEAD"))
    private void onTakeOutput(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        // Handle cleansing refund
        if (skillLeveling$isCleansing && !skillLeveling$cleansingRefundTome.isEmpty()) {
            // Give the player the extracted skill tome
            if (!player.getInventory().insertStack(skillLeveling$cleansingRefundTome.copy())) {
                // If inventory is full, drop it
                player.dropItem(skillLeveling$cleansingRefundTome.copy(), false);
            }
            skillLeveling$cleansingRefundTome = ItemStack.EMPTY;
        }

        // Vanilla handles consumption via repairItemUsage
        // However, if we need custom logic (e.g. consuming more than 1), we would do it
        // here.
        // For now, repairItemUsage = 1 is sufficient and handled by vanilla.
    }

    @Inject(method = "canTakeOutput", at = @At("HEAD"), cancellable = true)
    private void onCanTakeOutput(PlayerEntity player, boolean present, CallbackInfoReturnable<Boolean> cir) {
        if (skillLeveling$isSkillImbue || skillLeveling$isSigilSlotOpen || skillLeveling$isCleansing) {
            int cost = this.levelCost.get();

            if (player.getAbilities().creativeMode) {
                cir.setReturnValue(true);
                return;
            }

            if (cost > 0) {
                cir.setReturnValue(player.experienceLevel >= cost);
            } else {
                cir.setReturnValue(true);
            }
        }
    }
}
