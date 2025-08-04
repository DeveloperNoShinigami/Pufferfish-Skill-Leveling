package net.bluelotuscoding.puffishskillleveling.experience.source;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.puffish.skillsmod.api.SkillsAPI;

/**
 * Handles Forge events to feed data into experience sources.
 */
public final class ExperienceEvents {
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ExperienceEvents());
    }

    @SubscribeEvent
    public void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getState();
        ItemStack tool = player.getMainHandItem();
        SkillsAPI.updateExperienceSources(player, source -> {
            if (source instanceof BreakBlockExperienceSource breakSource) {
                return breakSource.getValue(player, state, tool);
            }
            if (source instanceof MineBlockExperienceSource mineSource) {
                return mineSource.getValue(player, state, tool);
            }
            return 0;
        });
    }
}

