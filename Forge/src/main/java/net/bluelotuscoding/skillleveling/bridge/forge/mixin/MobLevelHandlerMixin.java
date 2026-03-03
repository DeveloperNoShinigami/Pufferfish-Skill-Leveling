package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;

/**
 * Mixin to block native Epic Class XP gain and sync packets.
 * 
 * Epic Class has a MobLevelHandler that calculates its own XP and sends a
 * SycnLevelPacket directly when a mob dies. Since we are redirecting all XP
 * to Pufferfish, we need to block this native packet to avoid dual/incorrect
 * XP notifications in the HUD.
 */
@Mixin(targets = "com.example.epicclassmod.event.MobLevelHandler", remap = false)
public abstract class MobLevelHandlerMixin {

    @Inject(method = "onLivingDeath", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onLivingDeath(LivingDeathEvent e, CallbackInfo ci) {
        if (EpicClassBridge.isEnabled()) {
            // Cancel the native Epic Class XP gain and sync.
            // Our Bridge (via PufferfishExperienceMixin) will handle the sync
            // once Pufferfish processes the same death event.
            ci.cancel();
        }
    }
}
