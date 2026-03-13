package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.server.network.ServerPlayerEntity;
import net.bluelotuscoding.skillleveling.bridge.forge.EpicClassBridgeForgeAccess;

/**
 * Mixin to redirect Epic Class passive unlock checks to Pufferfish Skills.
 * 
 * This ensures that a passive is only considered "unlocked" if the
 * corresponding
 * Pufferfish skill has been purchased/unlocked by the player.
 */
@Pseudo
@Mixin(targets = "com.example.epicclassmod.passives.common.PassiveUnlocks", remap = false)
public abstract class PassiveUnlocksMixin {

    /**
     * Redirects requiredLevel(ClassType, int) to Pufferfish/Bridge Config.
     */
    @Inject(method = "requiredLevel(Lcom/example/epicclassmod/data/PlayerClassData$ClassType;I)I", at = @At("HEAD"), cancellable = true, remap = false)
    private static void puffish_onRequiredLevel(@Coerce Object type, int slot, CallbackInfoReturnable<Integer> cir) {
        if (!EpicClassBridge.isEnabled()) {
            return;
        }

        if (type instanceof Enum) {
            String className = ((Enum<?>) type).name();
            if (EpicClassBridge.isPassiveMapped(className, slot)) {
                cir.setReturnValue(EpicClassBridge.getRequiredLevel(className, slot));
            }
        }
    }

    /**
     * Redirects unlocked(ServerPlayer, ClassType, int) to Pufferfish.
     * Primary entry point for server-side passive status checks.
     */
    @Inject(method = "unlocked(Lnet/minecraft/server/level/ServerPlayer;Lcom/example/epicclassmod/data/PlayerClassData$ClassType;I)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void puffish_onUnlocked(@Coerce Object sp, @Coerce Object type, int slot,
            CallbackInfoReturnable<Boolean> cir) {
        if (!EpicClassBridge.isEnabled()) {
            return;
        }

        // Get class name from enum
        if (type instanceof Enum) {
            String className = ((Enum<?>) type).name();

            // Only override if this passive is explicitly mapped to a Pufferfish skill
            if (net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.isPassiveMapped(className, slot)) {
                boolean isUnlocked = net.bluelotuscoding.skillleveling.bridge.EpicClassBridge
                        .isPassiveUnlocked(sp, className, slot);
                cir.setReturnValue(isUnlocked);
            }
        }
    }

    /**
     * Redirects isUnlocked(ServerPlayer, ClassType, int) to Pufferfish.
     * Some parts of the mod call this wrapper instead of the direct unlocked()
     * method.
     */
    @Inject(method = "isUnlocked(Lnet/minecraft/server/level/ServerPlayer;Lcom/example/epicclassmod/data/PlayerClassData$ClassType;I)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void puffish_onIsUnlocked(@Coerce Object sp, @Coerce Object type, int slot,
            CallbackInfoReturnable<Boolean> cir) {
        if (!EpicClassBridge.isEnabled()) {
            return;
        }

        if (type instanceof Enum) {
            String className = ((Enum<?>) type).name();
            if (net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.isPassiveMapped(className, slot)) {
                boolean isUnlocked = net.bluelotuscoding.skillleveling.bridge.EpicClassBridge
                        .isPassiveUnlocked(sp, className, slot);
                cir.setReturnValue(isUnlocked);
            }
        }
    }
}
