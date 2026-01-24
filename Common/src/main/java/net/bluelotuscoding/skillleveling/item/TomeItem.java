package net.bluelotuscoding.skillleveling.item;

import net.bluelotuscoding.skillleveling.tome.TomePendingActionManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base Tome item for skill level modification.
 * 
 * When used, prompts the player via chat to select:
 * - Category (all tomes)
 * - Skill (Clear Mind tomes)
 * - Amount to refund (Greater Clear Mind only)
 */
public class TomeItem extends Item {

    public enum TomeType {
        PROGRESSION, // +1 category level (grants point)
        CLEAR_MIND, // -1 skill level (requires skill selection)
        GREATER_CLEAR_MIND // Refund N skill levels (requires skill + amount selection)
    }

    private final TomeType tomeType;

    public TomeItem(Settings settings, TomeType tomeType) {
        super(settings);
        this.tomeType = tomeType;
    }

    public TomeType getTomeType() {
        return tomeType;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            // Client-side: Just show a message, actual processing is server-side
            return TypedActionResult.success(stack);
        }

        // Server-side: Start the chat-based selection flow
        if (user instanceof ServerPlayerEntity serverPlayer) {
            // Check if player already has a pending action
            if (TomePendingActionManager.hasPendingAction(serverPlayer)) {
                serverPlayer.sendMessage(
                        Text.literal("§cYou already have a pending tome action. Type 'cancel' to cancel it."), false);
                return TypedActionResult.fail(stack);
            }

            // Start the tome action
            TomePendingActionManager.startTomeAction(serverPlayer, tomeType);
            return TypedActionResult.success(stack);
        }

        return TypedActionResult.pass(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        switch (tomeType) {
            case PROGRESSION -> {
                tooltip.add(Text.translatable("item.puffish_skill_leveling.tome_of_progression.desc1")
                        .formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("item.puffish_skill_leveling.tome_of_progression.desc2")
                        .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
            }
            case CLEAR_MIND -> {
                tooltip.add(Text.translatable("item.puffish_skill_leveling.tome_of_clear_mind.desc1")
                        .formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("item.puffish_skill_leveling.tome_of_clear_mind.desc2")
                        .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
            }
            case GREATER_CLEAR_MIND -> {
                tooltip.add(Text.translatable("item.puffish_skill_leveling.tome_of_greater_clear_mind.desc1")
                        .formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("item.puffish_skill_leveling.tome_of_greater_clear_mind.desc2")
                        .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
            }
            default -> {
            }
        }
    }

    /**
     * Consume one tome from the player's hand after successful use.
     */
    public static void consumeTome(ServerPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
    }
}
