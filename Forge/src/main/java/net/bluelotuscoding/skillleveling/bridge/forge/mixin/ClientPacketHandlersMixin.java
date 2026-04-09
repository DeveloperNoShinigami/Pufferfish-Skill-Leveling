package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import net.bluelotuscoding.skillleveling.bridge.BridgeConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.example.epicclassmod.client.ClientPacketHandlers", remap = false)
public class ClientPacketHandlersMixin {

    private static boolean hasShownQuestThisSession = false;

    @Inject(method = "handleOpenDialogue", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onHandleOpenDialogue(@Coerce Object pkt, CallbackInfo ci) {
        try {
            java.lang.reflect.Method npcIdMethod = pkt.getClass().getMethod("npcId");
            String npcId = (String) npcIdMethod.invoke(pkt);
            if ("main__gui.epicclassmod.quest.main.1".equals(npcId)) {
                if (BridgeConfigManager.getConfig() != null && BridgeConfigManager.getConfig().useCnpcQuests) {
                    ci.cancel();
                    return;
                }
                try {
                    Class<?> stateClass = Class.forName("com.example.epicclassmod.client.ClientClassState");
                    Object selectedType = stateClass.getField("selectedType").get(null);
                    if (selectedType != null && !"NONE".equals(selectedType.toString())) {
                        ci.cancel();
                        return; // Player already has a class, block quest
                    }
                } catch (Exception ex) {
                    // Ignore reflection errors
                }

                if (hasShownQuestThisSession) {
                    ci.cancel();
                } else {
                    hasShownQuestThisSession = true;
                }
            }
        } catch (Exception ignored) {
            // If reflection fails, allow the packet through
        }
    }
}
