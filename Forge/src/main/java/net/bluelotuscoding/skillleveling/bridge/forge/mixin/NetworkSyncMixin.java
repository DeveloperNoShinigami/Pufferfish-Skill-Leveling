package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.example.epicclassmod.network.ModNetwork", remap = false)
public class NetworkSyncMixin {

    /**
     * Intercept ModNetwork.sendTo
     */
    @Inject(method = "sendTo", at = @At("HEAD"), cancellable = true)
    private static void onSendTo(net.minecraft.server.network.ServerPlayerEntity sp, Object msg, CallbackInfo ci) {
        try {
            Class<?> syncPacketClass = Class.forName("com.example.epicclassmod.network.SyncClassPacket");
            if (syncPacketClass.isInstance(msg)) {
                String customId = net.bluelotuscoding.skillleveling.bridge.data.CustomClassData.getCustomClass(sp);
                java.lang.reflect.Method playerIdMethod = syncPacketClass.getMethod("playerId");
                java.util.UUID uuid = (java.util.UUID) playerIdMethod.invoke(msg);
                
                // Send our custom string packet instead via our Forge network handler
                net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler.CHANNEL.sendTo(
                        new net.bluelotuscoding.skillleveling.bridge.network.CustomSyncClassPacket(uuid, customId),
                        sp.networkHandler.connection,
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Intercept ModNetwork.sendToTrackingAndSelf
     */
    @Inject(method = "sendToTrackingAndSelf", at = @At("HEAD"), cancellable = true)
    private static void onSendToTrackingAndSelf(net.minecraft.server.network.ServerPlayerEntity sp, Object msg, CallbackInfo ci) {
        try {
            Class<?> syncPacketClass = Class.forName("com.example.epicclassmod.network.SyncClassPacket");
            if (syncPacketClass.isInstance(msg)) {
                String customId = net.bluelotuscoding.skillleveling.bridge.data.CustomClassData.getCustomClass(sp);
                java.lang.reflect.Method playerIdMethod = syncPacketClass.getMethod("playerId");
                java.util.UUID uuid = (java.util.UUID) playerIdMethod.invoke(msg);

                net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                        new net.bluelotuscoding.skillleveling.bridge.network.CustomSyncClassPacket(uuid, customId));
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}
