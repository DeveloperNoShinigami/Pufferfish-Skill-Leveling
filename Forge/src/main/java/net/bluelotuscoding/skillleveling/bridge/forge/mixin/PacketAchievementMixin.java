package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import net.bluelotuscoding.skillleveling.bridge.forge.client.cnpc.CnpcClientBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "noppes.npcs.packets.client.PacketAchievement", remap = false)
public class PacketAchievementMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void addon$suppressCnpcQuestAcceptToast(CallbackInfo ci) {
        if (CnpcClientBridge.isQuestAcceptAchievement(this) || CnpcClientBridge.isQuestCompleteAchievement(this)) {
            ci.cancel();
        }
    }
}
