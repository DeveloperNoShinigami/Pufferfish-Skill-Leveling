package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import java.util.ArrayList;
import java.util.HashSet;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestEpicClassAdapter;
import net.bluelotuscoding.skillleveling.client.CnpcClientQuestState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.example.epicclassmod.client.QuestPreviewOverlay", remap = false)
public class QuestPreviewOverlayMixin {

    @Inject(method = "safeAcceptedKeys", at = @At("HEAD"), cancellable = true)
    private static void addon$cnpcAcceptedKeys(CallbackInfoReturnable<java.util.List<String>> cir) {
        if (CnpcClientQuestState.isEnabled()) {
            CnpcQuestEpicClassAdapter.clearOverlayRenderContext();
            cir.setReturnValue(new ArrayList<>(CnpcClientQuestState.getAcceptedKeys()));
        }
    }

    @Inject(method = "loadCompletedKeysPreferClientCache", at = @At("HEAD"), cancellable = true)
    private static void addon$cnpcCompletedKeys(@Coerce Object player, CallbackInfoReturnable<java.util.Set<String>> cir) {
        if (CnpcClientQuestState.isEnabled()) {
            cir.setReturnValue(new HashSet<>(CnpcClientQuestState.getCompletedKeys()));
        }
    }

    @Inject(method = "resolveQuestDef", at = @At("HEAD"), cancellable = true)
    private static void addon$cnpcResolveQuest(@Coerce Object player, String titleKey, java.util.Set<String> completed,
            CallbackInfoReturnable cir) {
        if (CnpcClientQuestState.isEnabled()) {
            Object questDef = CnpcQuestEpicClassAdapter.resolveOverlayQuestDef(titleKey);
            if (questDef != null) {
                cir.setReturnValue(questDef);
            }
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/example/epicclassmod/client/QuestPreviewOverlay;drawScaled(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIF)V"), remap = false)
    private static void addon$cnpcOverlayLineColor(DrawContext context, TextRenderer font, String text, int x, int y,
            int color, float scale) {
        int resolvedColor = CnpcClientQuestState.isEnabled()
                ? CnpcQuestEpicClassAdapter.resolveOverlayLineColor(text, color)
                : color;
        context.getMatrices().push();
        context.getMatrices().translate((float) x, (float) y, 300.0f);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.drawText(font, text, 0, 0, resolvedColor, false);
        context.getMatrices().pop();
    }
}
