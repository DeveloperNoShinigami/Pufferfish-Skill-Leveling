package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import com.example.epicclassmod.event.ClassSyncTrackingEvents;
import com.example.epicclassmod.event.CommonEvents;
import com.example.epicclassmod.event.ClassPersistEvents;
import com.example.epicclassmod.network.ChooseClassPacket;
import com.example.epicclassmod.network.ModNetwork;
import com.example.epicclassmod.network.SyncClassPacket;
import net.bluelotuscoding.skillleveling.bridge.data.CustomClassData;
import net.bluelotuscoding.skillleveling.bridge.network.CustomSyncClassPacket;
import net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModNetwork.class, remap = false)
public class NetworkSyncMixin {

    /**
     * Intercept ModNetwork.sendTo
     */
    @Inject(method = "sendTo", at = @At("HEAD"), cancellable = true)
    private static void onSendTo(ServerPlayerEntity sp, Object msg, CallbackInfo ci) {
        if (msg instanceof SyncClassPacket syncPacket) {
            String customId = CustomClassData.getCustomClass(sp);
            // Send our custom string packet instead via our Forge network handler
            net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler.CHANNEL.sendTo(
                    new CustomSyncClassPacket(syncPacket.playerId(), customId),
                    sp.networkHandler.connection,
                    net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
            // We still let the original packet go through so ANY hardcoded core mod
            // features depending on it don't break,
            // but we send ours alongside it so our custom string state updates the client
            // as well.
        }
    }

    /**
     * Intercept ModNetwork.sendToTrackingAndSelf
     */
    @Inject(method = "sendToTrackingAndSelf", at = @At("HEAD"), cancellable = true)
    private static void onSendToTrackingAndSelf(ServerPlayerEntity sp, Object msg, CallbackInfo ci) {
        if (msg instanceof SyncClassPacket syncPacket) {
            String customId = CustomClassData.getCustomClass(sp);
            net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                    new CustomSyncClassPacket(syncPacket.playerId(), customId));
            // Let original packet go through as well.
        }
    }
}
