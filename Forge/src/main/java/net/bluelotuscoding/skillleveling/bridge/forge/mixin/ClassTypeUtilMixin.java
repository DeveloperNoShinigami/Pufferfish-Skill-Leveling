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
            // Get the player's actual custom class name
            String className = SkillLevelingMod.getInstance().getPlatform().getEpicClassName(sp);
            if (className == null || className.isEmpty()) {
                return;
            }

            // Look up the class definition to check for epic_class_proxy
            EpicClassDef def = EpicClassConfigManager.getClassDef(className);
            if (def != null && def.epic_class_proxy != null && !def.epic_class_proxy.isEmpty()) {
                // This is a custom class using a proxy — return null to block all proxy
                // passives
                cir.setReturnValue(null);
            }
        } catch (Exception ignored) {
            // Fail-safe: if anything goes wrong, let natural behavior proceed
        }
    }
}
