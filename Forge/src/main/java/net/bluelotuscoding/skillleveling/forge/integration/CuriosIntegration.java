package net.bluelotuscoding.skillleveling.forge.integration;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

/**
 * Handles Curios-specific events on the Forge platform.
 */
public class CuriosIntegration {

    @SubscribeEvent
    public void onCurioChange(CurioChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity serverPlayer) {
            // Curios changes can affect skill levels via equipment bonuses.
            // Refresh rewards to ensure attributes and other effects are updated.
            SkillLevelingMod.getInstance().getSkillLevelingManager().refreshAllRewards(serverPlayer);
        }
    }
}
