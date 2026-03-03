package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import net.bluelotuscoding.skillleveling.bridge.forge.EpicClassSyncHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.example.epicclassmod.network.ResetStatsPacket", remap = false)
public class ResetStatsPacketMixin {

    /**
     * Redirects the sync lambda in ResetStatsPacket.handle to call our forceSync,
     * which handles both vanilla level sync AND our custom attribute/NBT sync.
     */
    @Redirect(method = "lambda$handle$0", at = @At(value = "INVOKE", target = "Lcom/example/epicclassmod/network/ModNetwork;syncLevelToClient(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private static void addon$redirectToForceSync(@Coerce Object sp) {
        if (sp != null) {
            EpicClassSyncHelper.applyModifiers(sp);
            EpicClassSyncHelper.forceSync(sp);
        }
    }
}
