package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestEpicClassAdapter;
import net.bluelotuscoding.skillleveling.bridge.forge.client.cnpc.CnpcClientBridge;
import net.bluelotuscoding.skillleveling.client.CnpcClientQuestState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.example.epicclassmod.client.ClassBookQuestsPage", remap = false)
public class ClassBookQuestsPageMixin {

    // Captures the titleKey of the currently-selected quest during render's short-circuit AND check.
    // isAcceptedClient(titleKey) fires just before canTrackStructureForCurrentClass(), so we store
    // the key here and read it in the structureFor redirects — avoids fragile getSelected() reflection.
    @Unique
    private static String cnpcRenderFrameQuestKey = null;

    @Inject(method = "render", at = @At("HEAD"))
    private void addon$cnpcBeginBookRenderContext(CallbackInfo ci) {
        if (!CnpcClientQuestState.isEnabled()) {
            return;
        }
        // Pre-populate the key so canTrackStructureForCurrentClass() redirect has it available
        // even if the isAcceptedClient redirect fires in a different order.
        cnpcRenderFrameQuestKey = resolveSelectedKey();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void addon$cnpcEndBookRenderContext(CallbackInfo ci) {
    }

    @Inject(method = "getJobQuests", at = @At("HEAD"), cancellable = true)
    private static void addon$cnpcJobQuests(@Coerce Object cls, CallbackInfoReturnable cir) {
        if (CnpcClientQuestState.isEnabled()) {
            String currentClass = addon$resolveJobClassId(cls);
            cir.setReturnValue(CnpcQuestEpicClassAdapter.getJobQuestDefs(currentClass));
        }
    }

    @Inject(method = "jobLabelKey", at = @At("HEAD"), cancellable = true)
    private static void addon$customJobLabelKey(@Coerce Object cls, CallbackInfoReturnable<String> cir) {
        String labelKey = CnpcQuestEpicClassAdapter.getCurrentClientClassLabelKey();
        if (labelKey != null && !labelKey.isBlank()) {
            cir.setReturnValue(labelKey);
        }
    }

    @Inject(method = "jobFallbackLabel", at = @At("HEAD"), cancellable = true)
    private static void addon$customJobFallbackLabel(@Coerce Object cls, CallbackInfoReturnable<String> cir) {
        String label = CnpcQuestEpicClassAdapter.getCurrentClientClassLabel();
        if (label != null && !label.isBlank()) {
            cir.setReturnValue(label);
        }
    }

    @Inject(method = "subAllForDisplay", at = @At("HEAD"), cancellable = true)
    private static void addon$cnpcSideQuests(CallbackInfoReturnable cir) {
        if (CnpcClientQuestState.isEnabled()) {
            cir.setReturnValue(CnpcQuestEpicClassAdapter.getSideQuestDefs());
        }
    }

    @Inject(method = "isCompleted", at = @At("HEAD"), cancellable = true)
    private void addon$cnpcQuestCompleted(@Coerce Object q, CallbackInfoReturnable<Boolean> cir) {
        if (!CnpcClientQuestState.isEnabled() || q == null) {
            return;
        }
        String questKey = extractQuestKey(q);
        if (questKey != null) {
            cir.setReturnValue(CnpcClientQuestState.isCompleted(questKey));
        }
    }

    @Inject(method = "hasAllRequirements", at = @At("HEAD"), cancellable = true)
    private static void addon$cnpcHasAllRequirements(PlayerEntity player, @Coerce Object q,
            CallbackInfoReturnable<Boolean> cir) {
        if (!CnpcClientQuestState.isEnabled() || q == null) {
            return;
        }
        String questKey = extractQuestKey(q);
        if (questKey != null && CnpcQuestEpicClassAdapter.isMirroredQuestKey(questKey)) {
            cir.setReturnValue(CnpcQuestEpicClassAdapter.isReadyToTurnIn(questKey));
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/example/epicclassmod/client/ClassBookQuestsPage;hasAllRequirements(Lnet/minecraft/entity/player/PlayerEntity;Lcom/example/epicclassmod/data/quest/QuestDef;)Z"), remap = false)
    private boolean addon$cnpcRenderHasAllRequirements(PlayerEntity player, @Coerce Object q) {
        if (CnpcClientQuestState.isEnabled()) {
            String questKey = extractQuestKey(q);
            if (questKey != null && CnpcQuestEpicClassAdapter.isMirroredQuestKey(questKey)) {
                return CnpcQuestEpicClassAdapter.isReadyToTurnIn(questKey);
            }
        }
        return invokeHasAllRequirements(player, q);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 0))
    private boolean addon$cnpcTextOnlyBookRequirements(java.util.List<?> list) {
        if (CnpcClientQuestState.isEnabled() && CnpcQuestEpicClassAdapter.hasTextOnlyRequirements(resolveSelectedKey())) {
            return true;
        }
        return list.isEmpty();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 2))
    private boolean addon$cnpcTreatTextOnlyQuestAsCompletableList(java.util.List<?> list) {
        if (CnpcClientQuestState.isEnabled() && CnpcQuestEpicClassAdapter.hasTextOnlyRequirements(resolveSelectedKey())) {
            return false;
        }
        return list.isEmpty();
    }

    @Inject(method = "abandonSelectedQuest", at = @At("HEAD"), cancellable = true)
    private void addon$cnpcAbandonSelectedQuest(CallbackInfo ci) {
        if (!CnpcClientQuestState.isEnabled()) {
            return;
        }
        String key = resolveSelectedKey();
        if (key == null) {
            return;
        }
        var view = CnpcClientQuestState.getQuest(key);
        if (view == null || view.questId == null || view.questId.isBlank()) {
            return;
        }
        ci.cancel();
        CnpcClientQuestState.removeAccepted(view.questId);
        CnpcClientBridge.sendQuestAbandon(view.questId);
    }

    // Capture the selected quest's titleKey right before canTrackStructureForCurrentClass() fires.
    // isAcceptedClient(titleKey) is the condition immediately preceding it in ECM's render AND-chain.
    // In CNPC mode, we own the acceptance state — return it directly. This avoids fragile reflection
    // that silently returns false on any error, which would short-circuit showTrack before the
    // canTrackStructureForCurrentClass() redirect can even evaluate the per-quest trackStructure.
    // ECM's tracker is class-based (structureFor(cls) switch); CNPC tracker is per-quest — so we
    // must control acceptance here to keep canTrackStructureForCurrentClass() reachable.
    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/example/epicclassmod/client/ClassBookQuestsPage;isAcceptedClient(Ljava/lang/String;)Z",
                    remap = false))
    private boolean addon$cnpcCaptureRenderKey(String titleKey) {
        cnpcRenderFrameQuestKey = titleKey;
        if (CnpcClientQuestState.isEnabled()) {
            // In CNPC mode: directly check our own acceptance state — no ECM native routing needed.
            return CnpcClientQuestState.isAccepted(titleKey);
        }
        // Non-CNPC mode: replicate ECM's call through ClientQuestState via reflection.
        try {
            Class<?> pageClass = Class.forName("com.example.epicclassmod.client.ClassBookQuestsPage");
            java.lang.reflect.Method m = pageClass.getDeclaredMethod("isAcceptedClient", String.class);
            m.setAccessible(true);
            return Boolean.TRUE.equals(m.invoke(null, titleKey));
        } catch (Exception e) {
            return false;
        }
    }

    // Inject at HEAD of canTrackStructureForCurrentClass() to override its return value.
    // @Inject at HEAD with cancellable=true is the most reliable Mixin operation — it works
    // on private methods with @Pseudo, unlike @Redirect inside a private method body which
    // can silently fail. ECM's tracker is class-based (switch on JobClass enum); CNPC tracker
    // is per-quest. We return true when the selected CNPC quest has a trackStructure set.
    @Inject(method = "canTrackStructureForCurrentClass", at = @At("HEAD"), cancellable = true)
    private void addon$cnpcOverrideCanTrack(CallbackInfoReturnable<Boolean> cir) {
        if (!CnpcClientQuestState.isEnabled()) {
            return;
        }
        String key = cnpcRenderFrameQuestKey;
        if (key == null) {
            key = resolveSelectedKey();
        }
        var view = key != null ? CnpcClientQuestState.getQuest(key) : null;
        if (view != null) {
            cir.setReturnValue(view.trackStructure != null && !view.trackStructure.isBlank());
        }
    }

    // Redirect render()'s direct call to structureFor() — used for the glow/button-rect creation
    // after showTrack is true. Must also return our structure ID so the button rect is created.
    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/example/epicclassmod/client/ClassBookQuestsPage;structureFor(Lcom/example/epicclassmod/data/JobClass;)Ljava/lang/String;",
                    remap = false))
    private String addon$cnpcStructureForRender(@Coerce Object cls) {
        if (CnpcClientQuestState.isEnabled()) {
            String key = cnpcRenderFrameQuestKey;
            if (key == null) {
                key = resolveSelectedKey();
            }
            var view = key != null ? CnpcClientQuestState.getQuest(key) : null;
            if (view != null) {
                return view.trackStructure != null ? view.trackStructure : "";
            }
        }
        return invokeStructureFor(cls);
    }

    @Redirect(method = "onClickTrackBoss",
            at = @At(value = "INVOKE",
                    target = "Lcom/example/epicclassmod/client/ClassBookQuestsPage;structureFor(Lcom/example/epicclassmod/data/JobClass;)Ljava/lang/String;",
                    remap = false))
    private String addon$cnpcStructureForClick(@Coerce Object cls) {
        if (!CnpcClientQuestState.isEnabled()) {
            return invokeStructureFor(cls);
        }
        var view = cnpcRenderFrameQuestKey != null ? CnpcClientQuestState.getQuest(cnpcRenderFrameQuestKey) : null;
        if (view != null) {
            return view.trackStructure != null ? view.trackStructure : "";
        }
        return invokeStructureFor(cls);
    }

    // Reads ECM's this.currentClass field for non-CNPC fallback path
    private Object readCurrentClassField() {
        for (Class<?> c = this.getClass(); c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField("currentClass");
                f.setAccessible(true);
                return f.get(this);
            } catch (NoSuchFieldException ignored) {
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static String invokeStructureFor(@Coerce Object cls) {
        try {
            Class<?> pageClass = Class.forName("com.example.epicclassmod.client.ClassBookQuestsPage");
            Class<?> jobClassType = Class.forName("com.example.epicclassmod.data.JobClass");
            java.lang.reflect.Method m = pageClass.getDeclaredMethod("structureFor", jobClassType);
            m.setAccessible(true);
            Object result = m.invoke(null, cls);
            return result instanceof String s ? s : "";
        } catch (Exception e) {
            return "";
        }
    }

    @Inject(method = "completeSelectedQuest", at = @At("HEAD"), cancellable = true)
    private void addon$completeCnpcQuestFromBook(CallbackInfo ci) {
        if (!CnpcClientQuestState.isEnabled()) {
            return;
        }
        try {
            java.lang.reflect.Method getSelected = findMethod(this.getClass(), "getSelected");
            if (getSelected == null) {
                return;
            }
            Object selected = getSelected.invoke(this);
            if (selected == null) {
                return;
            }
            String stringKey = extractQuestKey(selected);
            if (stringKey == null) {
                return;
            }
            var view = CnpcClientQuestState.getQuest(stringKey);
            if (view == null || view.questId == null || view.questId.isBlank()) {
                return;
            }
            ci.cancel();
            if (!view.readyToTurnIn) {
                CnpcClientBridge.showQuestNotReady(view.title);
                return;
            }
            if (!CnpcClientBridge.sendQuestCompletionCheck(view.questId)) {
                CnpcClientBridge.showQuestNotReady(view.title);
            }
        } catch (Exception ignored) {
        }
    }

    private String resolveSelectedKey() {
        try {
            java.lang.reflect.Method getSelected = findMethod(this.getClass(), "getSelected");
            if (getSelected == null) {
                return null;
            }
            Object selected = getSelected.invoke(this);
            return extractQuestKey(selected);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String extractQuestKey(Object questDef) {
        return CnpcQuestEpicClassAdapter.resolveQuestKey(questDef);
    }

    private static java.lang.reflect.Method findMethod(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                java.lang.reflect.Method method = current.getDeclaredMethod(name);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static boolean invokeHasAllRequirements(PlayerEntity player, Object questDef) {
        try {
            Class<?> targetClass = Class.forName("com.example.epicclassmod.client.ClassBookQuestsPage");
            Class<?> questDefClass = Class.forName("com.example.epicclassmod.data.quest.QuestDef");
            java.lang.reflect.Method method = targetClass.getDeclaredMethod("hasAllRequirements", PlayerEntity.class,
                    questDefClass);
            method.setAccessible(true);
            return Boolean.TRUE.equals(method.invoke(null, player, questDef));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String addon$resolveJobClassId(Object cls) {
        String currentClass = CnpcQuestEpicClassAdapter.getCurrentClientClassId();
        if ((currentClass == null || currentClass.isBlank()) && cls != null) {
            currentClass = cls.toString();
        }
        return currentClass;
    }
}
