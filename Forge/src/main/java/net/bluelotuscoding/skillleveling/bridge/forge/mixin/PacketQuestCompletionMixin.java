package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import java.lang.reflect.Field;
import net.bluelotuscoding.skillleveling.bridge.forge.client.cnpc.CnpcClientBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "noppes.npcs.packets.client.PacketQuestCompletion", remap = false)
public class PacketQuestCompletionMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void addon$completeMirroredQuestWithoutCnpcGui(CallbackInfo ci) {
        String questId = readQuestId(this);
        if (!CnpcClientBridge.isMirroredQuest(questId)) {
            return;
        }
        if (CnpcClientBridge.sendQuestCompletionCheck(questId)) {
            ci.cancel();
        }
    }

    private static String readQuestId(Object packet) {
        try {
            Field field = packet.getClass().getDeclaredField("id");
            field.setAccessible(true);
            Object value = field.get(packet);
            return value == null ? null : value.toString();
        } catch (Exception ignored) {
            return null;
        }
    }
}
