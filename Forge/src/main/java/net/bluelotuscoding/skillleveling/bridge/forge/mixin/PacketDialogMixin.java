package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import net.bluelotuscoding.skillleveling.bridge.forge.client.cnpc.CnpcClientBridge;
import net.minecraft.entity.player.PlayerEntity;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "noppes.npcs.packets.client.PacketDialog", remap = false)
public class PacketDialogMixin {

    @Inject(method = "openDialog", at = @At("HEAD"), cancellable = true)
    private static void addon$openEpicClassDialog(Dialog dialog, EntityNPCInterface npc, PlayerEntity player,
            CallbackInfo ci) {
        if (!CnpcClientBridge.isMirroredNpc(npc)) {
            return;
        }
        if (CnpcClientBridge.openMirroredDialog(dialog, npc, player)) {
            ci.cancel();
        }
    }
}
