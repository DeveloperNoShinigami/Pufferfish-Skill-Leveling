package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to detect when the player changes their selected hotbar slot.
 * This allows real-time updates of skill rewards provided by hand-held items.
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onUpdateSelectedSlot", at = @At("RETURN"))
    private void onUpdateSelectedSlotChange(UpdateSelectedSlotC2SPacket packet, CallbackInfo ci) {
        if (player != null && !player.getWorld().isClient()) {
            var mod = SkillLevelingMod.getInstance();
            if (mod != null && mod.getSkillLevelingManager() != null) {
                // Refresh rewards when hotbar slot changes (for main hand bonuses)
                mod.getSkillLevelingManager().refreshAllRewards(player);
                // Also sync to client to ensure UI reflects new levels
                mod.getSkillLevelingManager().syncAllSkillsToPlayer(player);
            }
        }
    }
}
