package net.bluelotuscoding.skillleveling.forge.loot;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Handles direct injection of loot pools into loot tables.
 * This is a more "persistent" alternative to Global Loot Modifiers for entity
 * drops.
 */
public class LootInjectionHandler {

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.getWorld().isClient) {
            return;
        }

        Identifier lootTableId = entity.getLootTable();
        if (lootTableId == null) {
            return;
        }

        var imbueManager = SkillLevelingMod.getInstance().getLootImbueManager();
        // Note: For Mob Drops, we use the Entity directly and imbuement is handled
        // inside processEntry if we pass a LootContext, but here we provide a null
        // context
        // and manually imbue if needed.

        // We no longer inject items here because they are handled by Global Loot
        // Modifiers
        // in UniversalLootModifier.java. This ensures visibility to /loot and prevents
        // duplicates.

        // CRITICAL: Also imbue the core drops (like Vanilla Bows/Armor) that bypass
        // GLMs.
        // We iterate through all drops and imbue any that are NOT already imbued.
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getStack();
            if (!stack.isEmpty() && !net.bluelotuscoding.skillleveling.util.ImbuedSkillHelper.isImbued(stack)) {
                imbueManager.applyRandomImbue(stack, entity.getWorld(), entity.getPos(), entity,
                        entity.getWorld().getRandom(), lootTableId);
                // The item stack is modified in-place since we are accessing it directly via
                // getStack()
                // and imbuing logic writes NBT to it.
            }
        }
    }

}
