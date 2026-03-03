package net.bluelotuscoding.skillleveling.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class PuffishItemHelper {

    /**
     * Parses an item string that may optionally contain NBT data.
     * Format: namespace:path{nbt_data}
     * 
     * @param rawId The raw item string to parse
     * @return An ItemStack created from the parsed ID and NBT, or ItemStack.EMPTY
     *         on failure.
     */
    public static ItemStack parseItemStack(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return ItemStack.EMPTY;
        }

        try {
            String idPart = rawId;
            String nbtPart = null;

            // Check if there is NBT data (starts with {)
            int nbtStart = rawId.indexOf('{');
            if (nbtStart != -1) {
                idPart = rawId.substring(0, nbtStart);
                nbtPart = rawId.substring(nbtStart);
            }

            Identifier id = new Identifier(idPart);
            Item item = Registries.ITEM.get(id);

            if (item == Items.AIR) {
                // Secondary check for item consistency or mod-specific loading
                return ItemStack.EMPTY;
            }

            ItemStack stack = new ItemStack(item);

            if (nbtPart != null) {
                try {
                    NbtCompound nbt = StringNbtReader.parse(nbtPart);
                    stack.setNbt(nbt);
                } catch (Exception e) {
                    AddonLogger.LOGGER.error("Failed to parse NBT for item " + idPart + ": " + e.getMessage());
                }
            }

            return stack;
        } catch (Exception e) {
            AddonLogger.LOGGER.error("Error parsing item stack from: " + rawId + " - " + e.getMessage());
            return ItemStack.EMPTY;
        }
    }
}
