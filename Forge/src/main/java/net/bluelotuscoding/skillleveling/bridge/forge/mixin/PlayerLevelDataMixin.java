package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "com.example.epicclassmod.data.PlayerLevelData", remap = false)
public class PlayerLevelDataMixin {

    /**
     * Injects into the start of the resetAllocatedStatsAndRefund method to also
     * refund and clear
     * custom attributes defined by this addon.
     */
    @Inject(method = "resetAllocatedStatsAndRefund", at = @At("HEAD"))
    private static void addon$resetCustomStats(@Coerce Object sp, Runnable sync, CallbackInfo ci) {
        if (sp == null) {
            return;
        }

        try {
            Class<?> pldClass = Class.forName("com.example.epicclassmod.data.PlayerLevelData");
            Method rootMethod = pldClass.getDeclaredMethod("root", sp.getClass());
            rootMethod.setAccessible(true);
            NbtCompound tag = (NbtCompound) rootMethod.invoke(null, sp);

            if (tag == null) {
                return;
            }

            List<String> toRemove = new ArrayList<>();

            // 1. Identify all custom alloc_ tags
            for (String key : tag.getKeys()) {
                if (key.startsWith("alloc_")) {
                    // Skip vanilla stats as they are handled by the original method
                    if (key.equals("alloc_atk") || key.equals("alloc_def")
                            || key.equals("alloc_aspd") || key.equals("alloc_mspd")
                            || key.equals("alloc_cooldown") || key.equals("alloc_regen")) {
                        continue;
                    }
                    toRemove.add(key);
                }
            }

            // 2. Calculate refund and clear custom alloc tags
            int customRefund = 0;
            for (String key : toRemove) {
                customRefund += Math.max(0, tag.getInt(key));
                tag.remove(key);
            }

            if (customRefund > 0) {
                int currentSp = tag.getInt("stat_points");
                tag.putInt("stat_points", currentSp + customRefund);
            }
        } catch (Exception e) {
            AddonLogger.LOGGER.error("[Bridge] Failed to reset custom stats: " + e.getMessage());
        }
    }

    /**
     * Injects into the end of applyAllModifiers to ensure our custom attributes
     * are re-applied (or removed if points were refunded) immediately.
     */
    @Inject(method = "applyAllModifiers", at = @At("TAIL"))
    private static void addon$applyCustomModifiers(@Coerce Object sp, CallbackInfo ci) {
        if (sp instanceof ServerPlayerEntity spe) {
            net.bluelotuscoding.skillleveling.bridge.forge.EpicClassSyncHelper.applyCustomAttributes(spe);
        }
    }
}
