package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import com.example.epicclassmod.client.screen.DialogueScreen;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef;
import net.bluelotuscoding.skillleveling.bridge.network.CustomChooseClassPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DialogueScreen.class, remap = false)
public abstract class DialogueScreenMixin {

    @Shadow
    private String npcId;
    @Shadow
    private int entityId;
    @Shadow
    private boolean showAccepted;

    @Shadow(aliases = { "m_7379_", "close", "onClose" })
    public abstract void m_7379_();

    /**
     * @author Antigravity
     * @reason Intercept the dialog acceptance to assign our custom class.
     *         DialogueScreen.respond(boolean ok) handles sending
     *         RespondDialoguePacket
     *         and changing quest states.
     *         We want to inject at HEAD, check if it's an acceptance,
     *         and if the NPC maps to a custom class, we send OUR custom packet.
     */
    @Inject(method = "respond", at = @At("HEAD"), cancellable = true)
    private void onRespond(boolean ok, CallbackInfo ci) {
        if (!this.showAccepted && ok) {
            // Find which custom class this relates to
            String customClassId = getCustomClassForNpc(this.npcId);

            if (customClassId != null) {
                // Determine if we should send a normal quest respond packet
                // Actually, the core RespondDialoguePacket sets up the 'Accepted' state for
                // Quests.
                // But wait, the quest giving the class is NOT RespondDialoguePacket!
                // Let's analyze carefully.
                // If it is a class selection, we send our custom packet Instead of normal quest
                // acceptance.
                // Wait, if we send CustomChooseClassPacket, the server handles it. We should
                // return here.

                net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler.CHANNEL.sendToServer(
                        new CustomChooseClassPacket(customClassId));

                // We still want the UI to close.
                this.m_7379_();
                ci.cancel();
            }
        }
    }

    private static String getCustomClassForNpc(String npcId) {
        if (npcId == null) {
            return null;
        }

        // 1. Check if ANY custom class config references this npcId as its
        // job_master_id
        for (EpicClassDef def : EpicClassConfigManager.getClasses().values()) {
            if (def.job_master_id != null && def.job_master_id.equals(npcId)) {
                return def.class_name;
            }
        }

        // 2. Check if the npcId matches a class_name directly (legacy support)
        EpicClassDef directlyNamed = EpicClassConfigManager.getClassDef(npcId);
        if (directlyNamed != null) {
            return directlyNamed.class_name;
        }

        // Removed fallback to vanilla mappings. If this is a base class NPC,
        // returning null will let normal dialogue handling run and trigger the
        // native protocol for correct starting item grants.
        return null;
    }
}
