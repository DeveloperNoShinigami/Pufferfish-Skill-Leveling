package net.bluelotuscoding.skillleveling.forge.integration;

import net.bluelotuscoding.skillleveling.integration.EquipmentScanner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.List;

public class CuriosScanner implements EquipmentScanner {
    @Override
    public List<ItemStack> getExtraEquipment(LivingEntity entity) {
        List<ItemStack> stacks = new ArrayList<>();
        // Use getCuriosInventory if available or suppress warning
        CuriosApi.getCuriosHelper().getCuriosHandler(entity).ifPresent(handler -> {
            handler.getCurios().entrySet().forEach(entry -> {
                ICurioStacksHandler stacksHandler = entry.getValue();
                IDynamicStackHandler dynamicHandler = stacksHandler.getStacks();
                int slotCount = dynamicHandler.getSlots();

                for (int i = 0; i < slotCount; i++) {
                    ItemStack stack = dynamicHandler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        stacks.add(stack);
                    }
                }
            });
        });
        return stacks;
    }
}
