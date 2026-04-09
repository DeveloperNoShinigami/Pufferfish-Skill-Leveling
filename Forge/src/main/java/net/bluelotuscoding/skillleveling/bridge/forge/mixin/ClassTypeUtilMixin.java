package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Universal proxy passive blocker.
 *
 * Every passive handler in Epic Classes checks ClassTypeUtil.getType(sp) ==
 * <ClassType>.
 * By returning null for custom class players that have epic_class_proxy set,
 * ALL proxy passives and dashes are blocked universally with a single mixin.
 */
@Pseudo
@Mixin(targets = "com.example.epicclassmod.passives.common.ClassTypeUtil", remap = false)
public class ClassTypeUtilMixin {

    @Inject(method = "getType", at = @At("RETURN"), cancellable = true, remap = false)
    private static void puffish_overrideProxyType(@Coerce Object sp, CallbackInfoReturnable<?> cir) {
        if (!EpicClassBridge.isEnabled()) {
            return;
        }

        try {
            // Block ECM's native combat passives for any player using a custom class.
            // We no longer rely on epic_class_proxy — any custom class should suppress
            // the native enum-based passives since our system handles them instead.
            String className = SkillLevelingMod.getInstance().getPlatform().getEpicClassName(sp);
            if (className == null || className.isEmpty() || "epic_classes:none".equals(className)) {
                return;
            }
            EpicClassDef def = EpicClassConfigManager.getClassDef(className);
            if (def != null) {
                // Custom class is active — return null so ECM skips all its proxy passives
                cir.setReturnValue(null);
            }
        } catch (Exception ignored) {
            // Fail-safe: if anything goes wrong, let natural behaviour proceed
        }
    }
}
