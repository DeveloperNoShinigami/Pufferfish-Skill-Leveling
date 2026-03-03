package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.bluelotuscoding.skillleveling.bridge.forge.EpicClassSyncHelper;

/**
 * Mixin to intercept stat allocation and force a client sync.
 * This fixes the issue where the UI doesn't update immediately after spending a
 * point.
 */
@Mixin(targets = "com.example.epicclassmod.data.PlayerLevelData", remap = false, priority = 1000)
public abstract class PlayerLevelAllocateMixin {

    /**
     * Injects after allocate(ServerPlayer, Stat, int) to sync the new state to the
     * client.
     */
    @Inject(method = "allocate", at = @At("RETURN"), remap = false)
    private static void onAllocate(@Coerce Object playerObj, @Coerce Object stat, int amount, @Coerce Object sync,
            CallbackInfoReturnable<Boolean> cir) {
        if (!EpicClassBridge.isEnabled()) {
            return;
        }

        // Use mapping-agnostic check
        if (playerObj != null && playerObj.getClass().getName().contains("ServerPlayer")) {
            // Then sync level/stats (which triggers the screen refresh via our client
            // mixin)
            EpicClassSyncHelper.forceSync(playerObj);
        }
    }
}
