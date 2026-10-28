package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes suppressed XP gain and level-up overlay notifications.
 *
 * Some Epic Classes internal state can suppress notice rendering even when
 * the bridge's own forceSync path delivers the correct SyncLevelPacket.
 * This mixin overrides allowNotices() to return true when the bridge is active,
 * ensuring XP/level toasts are shown consistently.
 */
@Pseudo
@Mixin(targets = "com.example.epicclassmod.client.AnnouncementOverlay", remap = false)
public abstract class AnnouncementOverlayMixin {

    @Inject(method = "allowNotices", at = @At("RETURN"), cancellable = true, remap = false)
    private static void bridgeAllowNotices(CallbackInfoReturnable<Boolean> cir) {
        if (net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.isEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
