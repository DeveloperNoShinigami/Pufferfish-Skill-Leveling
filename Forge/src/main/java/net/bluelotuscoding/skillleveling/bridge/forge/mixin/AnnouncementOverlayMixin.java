package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes suppressed XP gain and level-up overlay notifications.
 *
 * BridgeConfigManager.syncLegacyEpicClassWeaponRestrictions() calls
 * ModSettings.setJobWeaponRestrEnabled(false) to prevent ECM's legacy weapon
 * restriction system from conflicting with the bridge. As a side effect,
 * AnnouncementOverlay.allowNotices() (which reads isJobWeaponRestrEnabled) returns
 * false, silently suppressing all XP gain and level-up toasts even though the bridge's
 * own forceSync path sends the correct SyncLevelPacket with lastGain > 0.
 *
 * This mixin overrides allowNotices() to return true when the bridge is active,
 * decoupling notification gating from the weapon restriction flag.
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
