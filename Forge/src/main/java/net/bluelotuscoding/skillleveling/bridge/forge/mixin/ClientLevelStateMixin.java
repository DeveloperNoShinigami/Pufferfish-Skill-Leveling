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

    @org.spongepowered.asm.mixin.Shadow
    public static int level;

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

    @Inject(method = "applySync(IIIIIIIIIII)V", at = @At("HEAD"), remap = false)
    private static void onApplySyncHead(int newLevel, int newXp, int newNeeded, int newSP, int aAtk, int aDef, int aAS,
            int aMS, int aCooldown, int aRegen, int lastGain, CallbackInfo ci) {
        // If the current level is 0 and there was no XP gain, it's likely an initial
        // login sync.
        // We set the level to the newLevel before the rest of the method executes
        // to prevent the `if (newLevel > oldLevel)` check from triggering the level-up
        // animation.
        if (level == 0 && lastGain == 0) {
            level = newLevel;
        }
    }
}
