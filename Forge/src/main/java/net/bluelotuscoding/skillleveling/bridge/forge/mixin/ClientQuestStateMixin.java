package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import java.util.HashSet;
import net.bluelotuscoding.skillleveling.client.CnpcClientQuestState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.example.epicclassmod.client.ClientQuestState", remap = false)
public class ClientQuestStateMixin {

    @Inject(method = "removeAccepted", at = @At("HEAD"))
    private static void addon$cnpcRemoveAccepted(String key, CallbackInfo ci) {
        if (CnpcClientQuestState.isEnabled()) {
            CnpcClientQuestState.removeAccepted(key);
        }
    }

    @Inject(method = "isAccepted", at = @At("HEAD"), cancellable = true)
    private static void addon$cnpcIsAccepted(String key, CallbackInfoReturnable<Boolean> cir) {
        if (CnpcClientQuestState.isEnabled()) {
            cir.setReturnValue(CnpcClientQuestState.isAccepted(key));
        }
    }

    @Inject(method = "getAcceptedKeys", at = @At("HEAD"), cancellable = true)
    private static void addon$cnpcAcceptedKeys(CallbackInfoReturnable<java.util.Set<String>> cir) {
        if (CnpcClientQuestState.isEnabled()) {
            cir.setReturnValue(new HashSet<>(CnpcClientQuestState.getAcceptedKeys()));
        }
    }

    @Inject(method = "getCompletedMainSet", at = @At("HEAD"), cancellable = true)
    private static void addon$cnpcCompletedKeys(CallbackInfoReturnable<java.util.Set<String>> cir) {
        if (CnpcClientQuestState.isEnabled()) {
            cir.setReturnValue(new HashSet<>(CnpcClientQuestState.getCompletedKeys()));
        }
    }

    @Inject(method = "isCompleted", at = @At("HEAD"), cancellable = true)
    private static void addon$cnpcIsCompleted(String key, CallbackInfoReturnable<Boolean> cir) {
        if (CnpcClientQuestState.isEnabled()) {
            cir.setReturnValue(CnpcClientQuestState.isCompleted(key));
        }
    }
}
