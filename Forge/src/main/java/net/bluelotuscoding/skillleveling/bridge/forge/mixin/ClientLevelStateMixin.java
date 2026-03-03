package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to ClientLevelState to globally correct the level for UI display.
 * This ensures that Level 0 (Pufferfish) is displayed as 0 even though
 * internally Epic Classes starts at Level 1.
 */
@Mixin(targets = "com.example.epicclassmod.client.ClientLevelState", remap = false)
public abstract class ClientLevelStateMixin {

    /**
     * Subtracts 1 from the incoming synced level before it's applied to the state.
     * This ensures that Epic Level 1 is displayed as Level 0 in all UI components.
     * argsOnly = true and ordinal = 0 targets the first int argument (newLevel).
     */
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "applySync", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private static int adjustLevel(int level) {
        // Return level directly as we are now 1-to-1 with Pufferfish
        return level;
    }
}
