package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestEpicClassAdapter;
import net.bluelotuscoding.skillleveling.client.CnpcClientQuestState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.example.epicclassmod.data.quest.MainQuestChain", remap = false)
public class MainQuestChainMixin {

    @Inject(method = "currentForDisplay", at = @At("HEAD"), cancellable = true)
    private static void addon$cnpcCurrentForDisplay(@Coerce Object player, java.util.Set<String> completed,
            CallbackInfoReturnable cir) {
        if (CnpcClientQuestState.isEnabled()) {
            cir.setReturnValue(CnpcQuestEpicClassAdapter.getMainQuestDefs());
        }
    }

    @Inject(method = "nextAfterCompletedKeys", at = @At("HEAD"), cancellable = true)
    private static void addon$cnpcHideNativeNextMain(java.util.Collection<String> completed, CallbackInfoReturnable cir) {
        if (CnpcClientQuestState.isEnabled()) {
            cir.setReturnValue(null);
        }
    }
}
