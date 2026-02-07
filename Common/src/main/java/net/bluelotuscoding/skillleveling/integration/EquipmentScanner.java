package net.bluelotuscoding.skillleveling.integration;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import java.util.List;

/**
 * Interface for scanning extra equipment slots such as Curios or Trinkets.
 * Allows platform-specific implementations (Forge/Fabric) to be used in Common
 * code.
 */
public interface EquipmentScanner {
    /**
     * Get a list of item stacks from extra equipment slots.
     * 
     * @param entity The entity to scan.
     * @return A list of ItemStacks found in extra slots.
     */
    List<ItemStack> getExtraEquipment(LivingEntity entity);
}
