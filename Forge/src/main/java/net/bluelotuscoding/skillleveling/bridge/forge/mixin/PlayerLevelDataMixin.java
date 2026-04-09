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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            List<String> commandRemoved = new ArrayList<>();

            // 1. Identify all custom alloc_ tags
            for (String key : tag.getKeys()) {
                if (key.startsWith("alloc_")) {
                    int current = Math.max(0, tag.getInt(key));
                    if (current > 0) {
                        commandRemoved.add(key);
                    }
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

            if (sp instanceof ServerPlayerEntity spe && !commandRemoved.isEmpty()) {
                addon$scheduleResetZeroCommands(spe, commandRemoved);
            }

            if (customRefund > 0) {
                int currentSp = tag.getInt("stat_points");
                tag.putInt("stat_points", currentSp + customRefund);
            }
        } catch (Exception e) {
            AddonLogger.LOGGER.error("[Bridge] Failed to reset custom stats: " + e.getMessage());
        }
    }

    private static void addon$runResetZeroCommands(ServerPlayerEntity player, List<String> removedAllocKeys) {
        String className = net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getPlatform()
                .getEpicClassName(player);
        var def = net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager.getClassDef(className);
        if (def == null) {
            return;
        }

        Map<String, String> slotCommands = new HashMap<>();
        for (var page : net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager
                .getPagesForClass(def.class_name)) {
            if (page.slots == null) {
                continue;
            }
            for (var slot : page.slots) {
                if (slot != null && slot.id != null && slot.command != null && !slot.command.isBlank()) {
                    slotCommands.put(slot.id, slot.command);
                }
            }
        }

        Set<String> executedCommands = new LinkedHashSet<>();

        for (String allocKey : removedAllocKeys) {
            if (!allocKey.startsWith("alloc_")) {
                continue;
            }
            String statId = allocKey.substring("alloc_".length());
            String commandTemplate = slotCommands.get(statId);
            if (commandTemplate == null) {
                continue;
            }

            try {
                String command = commandTemplate
                        .replace("{value}", "0")
                        .replace("{player}", player.getEntityName())
                        .trim();
                if (command.isEmpty() || !executedCommands.add(command)) {
                    continue;
                }
                player.getServer().getCommandManager().executeWithPrefix(
                        player.getCommandSource().withSilent(), command);
            } catch (Exception e) {
                // Command cleanup must never block reset/refund flow.
                AddonLogger.LOGGER.warn("[Bridge] Reset command cleanup failed for stat " + statId + ": "
                        + e.getMessage());
            }
        }
    }

    private static void addon$scheduleResetZeroCommands(ServerPlayerEntity player, List<String> removedAllocKeys) {
        // Defer command cleanup so commands like "roleveling refresh" run after reset writes complete.
        List<String> snapshot = new ArrayList<>(removedAllocKeys);
        player.getServer().execute(() -> addon$runResetZeroCommands(player, snapshot));
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
