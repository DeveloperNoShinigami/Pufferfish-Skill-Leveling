package net.bluelotuscoding.skillleveling.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import net.bluelotuscoding.skillleveling.tome.TomePendingActionManager;

import java.util.List;

/**
 * Experience Tome - A tome that grants raw Pufferfish experience.
 * 
 * NBT Structure:
 * - TomeId: The internal ID of the tome definition
 * - TomeLevel: The level of the tome variant
 * - ExperienceGranted: Pre-calculated XP amount to grant
 */
public class ExpTomeItem extends Item {

    public static final String NBT_TOME_ID = "TomeId";
    public static final String NBT_TOME_LEVEL = "TomeLevel";
    public static final String NBT_EXPERIENCE_GRANTED = "ExperienceGranted";
    public static final String NBT_NAME = "CustomName";
    public static final String NBT_RARITY = "Rarity";

    public ExpTomeItem(Settings settings) {
        super(settings);
    }

    /**
     * Gets the experience amount granted by this tome stack.
     */
    public static int getExperienceGranted(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getInt(NBT_EXPERIENCE_GRANTED) : 0;
    }

    /**
     * Creates an Experience Tome ItemStack.
     */
    public static ItemStack createExpTome(Item tomeItem, String tomeId, String name, String rarity, int level,
            int xpAmount) {
        ItemStack stack = new ItemStack(tomeItem);
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(NBT_TOME_ID, tomeId);
        nbt.putString(NBT_NAME, name);
        nbt.putString(NBT_RARITY, rarity);
        nbt.putInt(NBT_TOME_LEVEL, level);
        nbt.putInt(NBT_EXPERIENCE_GRANTED, xpAmount);
        return stack;
    }

    @Override
    public Text getName(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(NBT_NAME)) {
            String name = nbt.getString(NBT_NAME);
            int level = nbt.getInt(NBT_TOME_LEVEL);

            Formatting color = Formatting.byName(nbt.getString(NBT_RARITY));
            if (color == null)
                color = Formatting.WHITE;

            if (level > 0) {
                return Text.literal(name + " " + toRoman(level)).formatted(color);
            }
            return Text.literal(name).formatted(color);
        }
        return super.getName(stack);
    }

    private String toRoman(int level) {
        if (level < 1)
            return "";
        if (level >= 4000)
            return String.valueOf(level);
        StringBuilder sb = new StringBuilder();
        int[] values = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        String[] symbols = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
        for (int i = 0; i < values.length; i++) {
            while (level >= values[i]) {
                level -= values[i];
                sb.append(symbols[i]);
            }
        }
        return sb.toString();
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return TypedActionResult.pass(stack);
        }

        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_EXPERIENCE_GRANTED)) {
            serverPlayer.sendMessage(Text.translatable("skillleveling.tome.invalid"), false);
            return TypedActionResult.fail(stack);
        }

        // Check if player already has a pending action
        if (TomePendingActionManager.hasPendingAction(serverPlayer)) {
            serverPlayer.sendMessage(
                    Text.literal("§cYou already have a pending tome action. Type 'cancel' to cancel it."), false);
            return TypedActionResult.fail(stack);
        }

        // Start the Experience Tome flow (Category -> Quantity -> Execution)
        TomePendingActionManager.startTomeAction(serverPlayer, TomeItem.TomeType.EXPERIENCE);
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_EXPERIENCE_GRANTED)) {
            tooltip.add(Text.literal("Unconfigured Experience Tome")
                    .formatted(Formatting.RED, Formatting.ITALIC));
            return;
        }

        int xpAmount = nbt.getInt(NBT_EXPERIENCE_GRANTED);

        tooltip.add(Text.translatable("item.puffish_skill_leveling.experience_tome.desc1",
                Text.literal("+" + xpAmount + " Experience").formatted(Formatting.GREEN)));

        tooltip.add(Text.translatable("item.puffish_skill_leveling.experience_tome.use_hint")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
