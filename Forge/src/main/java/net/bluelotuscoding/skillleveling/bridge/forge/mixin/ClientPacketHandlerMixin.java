package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;

/**
 * Mixin to ClientPacketHandlers to force a screen refresh when level data is
 * synced.
 * This ensures that the ClassBookScreen updates its cached attribute values
 * immediately.
 */
@Mixin(targets = "com.example.epicclassmod.client.ClientPacketHandlers", remap = false)
public abstract class ClientPacketHandlerMixin {

    @Inject(method = "handleOpenClassScreen(Lcom/example/epicclassmod/network/OpenClassScreenPacket;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onHandleOpenClassScreen(@Coerce Object pkt, CallbackInfo ci) {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().setScreen(
                    new net.bluelotuscoding.skillleveling.bridge.forge.client.screen.CustomClassSelectScreen());
        });
        ci.cancel();
    }

    @Inject(method = "handleSyncLevel(Lcom/example/epicclassmod/network/SyncLevelPacket;)V", at = @At("RETURN"), remap = false)
    private static void onHandleSyncLevelReturn(@Coerce Object packet, CallbackInfo ci) {
        try {
            // Force refresh of ClassBookScreen if open to show updated state
            MinecraftClient mc = MinecraftClient.getInstance();
            Object screen = mc.currentScreen;

            if (screen != null && screen.getClass().getName().contains("ClassBookScreen")) {
                // Re-initialize the screen to refresh cached values
                Method initMethod = screen.getClass().getSuperclass().getDeclaredMethod("init", MinecraftClient.class,
                        int.class, int.class);
                initMethod.setAccessible(true);
                initMethod.invoke(screen, mc, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }
}
