package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.bluelotuscoding.skillleveling.bridge.forge.EpicClassBridgeForgeAccess;
import net.bluelotuscoding.skillleveling.bridge.forge.EpicClassSyncHelper;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import java.lang.reflect.Method;

/**
 * Mixin to sync Pufferfish experience changes to Epic Class.
 * 
 * When Pufferfish gains XP or Level, we trigger a sync to ECM
 * to ensure its internal Stat Points and NBT are updated accordingly.
 */
@Mixin(targets = "net.puffish.skillsmod.impl.ExperienceImpl", remap = false)
public abstract class PufferfishExperienceMixin {

    @Unique
    private static final AddonLogger LOGGER = new AddonLogger();

    /**
     * Injects at RETURN of addTotal(Object, int) to sync after XP is added.
     */
    @Inject(method = "addTotal", at = @At("RETURN"), remap = false)
    private void onAddTotalReturn(@Coerce Object player, int amount, CallbackInfo ci) {
        sync(player, amount);
    }

    /**
     * Injects at RETURN of setTotal(Object, int) to sync after level/XP is set
     * directly.
     * This covers commands like /puffish_skills level set.
     */
    @Inject(method = "setTotal", at = @At("RETURN"), remap = false)
    private void onSetTotalReturn(@Coerce Object player, int total, CallbackInfo ci) {
        sync(player, 0);
    }

    @Unique
    private void sync(Object player, int lastGain) {
        if (!EpicClassBridge.isEnabled() || !EpicClassBridgeForgeAccess.isServerPlayer(player)) {
            return;
        }

        try {
            // "this" is the Experience instance
            Object experience = (Object) this;

            // Get current Level and XP from Pufferfish
            Integer level = invokeInt(experience, "getLevel", player);
            Integer currentXp = invokeInt(experience, "getCurrent", player);

            if (level != null && currentXp != null) {
                LOGGER.info("[Bridge] Pufferfish level/XP change detected. Triggering sync for " + player + ". Level="
                        + level + ", Gain=" + lastGain);
                // Sync to Epic Class
                EpicClassSyncHelper.syncFromPufferfish(
                        (net.minecraft.server.network.ServerPlayerEntity) player,
                        level,
                        currentXp,
                        lastGain);
            }

        } catch (Exception e) {
            LOGGER.warn("[Bridge] Error in PufferfishExperienceMixin: " + e.getMessage());
        }
    }

    private static Integer invokeInt(Object target, String methodName, Object player) {
        try {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                if (method.getParameterCount() != 1) {
                    continue;
                }
                if (!method.getParameterTypes()[0].isAssignableFrom(player.getClass())) {
                    continue;
                }

                Object result = method.invoke(target, player);
                if (result instanceof Integer) {
                    return (Integer) result;
                }
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
        return null;
    }
}
