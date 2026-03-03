package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.bluelotuscoding.skillleveling.bridge.forge.EpicClassSyncHelper;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Mixin into AllocateStatPacket.handle to force attribute sync after stat
 * allocation.
 *
 * Strategy: The original handle() calls enqueueWork() which schedules the
 * allocation on the server thread. We inject at RETURN of handle() and
 * enqueue our OWN work via the same context. Since the server processes
 * tasks in order, our sync will run AFTER the allocation completes.
 */
@Mixin(targets = "com.example.epicclassmod.network.AllocateStatPacket", remap = false)
public class AllocateStatPacketMixin {

    @SuppressWarnings("rawtypes")
    @Inject(method = "handle", at = @At("RETURN"), remap = false)
    private static void onHandleReturn(
            @Coerce Object packet,
            Supplier ctx,
            CallbackInfo ci) {
        if (!EpicClassBridge.isEnabled()) {
            return;
        }

        try {
            // ctx is Supplier<NetworkEvent.Context> - use raw types to avoid Forge imports
            Object context = ctx.get();

            // Reflectively call getSender() to get the ServerPlayer
            Method getSender = context.getClass().getMethod("getSender");
            Object sender = getSender.invoke(context);

            if (sender instanceof ServerPlayerEntity player) {
                net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER.debug(
                        "AllocateStatPacketMixin.onHandleReturn triggered for "
                                + player.getName().getString());

                // enqueueWork schedules on the server thread AFTER the packet's own work
                Method enqueueWork = null;
                for (Method m : context.getClass().getMethods()) {
                    if (m.getName().equals("enqueueWork") && m.getParameterCount() == 1) {
                        enqueueWork = m;
                        break;
                    }
                }

                if (enqueueWork != null) {
                    final ServerPlayerEntity finalPlayer = player;
                    enqueueWork.invoke(context, (Runnable) () -> {
                        net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER.debug(
                                "Running post-allocation sync for "
                                        + finalPlayer.getName().getString());
                        EpicClassSyncHelper.forceSync(finalPlayer);
                    });
                } else {
                    net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER
                            .debug("Could not find enqueueWork method, syncing directly");
                    // Fallback: sync directly (may be slightly early but better than nothing)
                    EpicClassSyncHelper.forceSync(player);
                }
            }
        } catch (Exception e) {
            net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER
                    .error("AllocateStatPacketMixin error: " + e.getMessage());
        }
    }
}
