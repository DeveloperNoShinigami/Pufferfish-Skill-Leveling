package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

@Pseudo
@Mixin(targets = "com.example.epicclassmod.network.ChooseClassPacket", remap = false)
public abstract class ChooseClassPacketMixin {

    /**
     * @author Antigravity
     * @reason Ensure base classes getting chosen via native packet still trigger
     *         the skill level reset logic.
     */
    @Inject(method = "handle", at = @At("TAIL"))
    private static void onHandleClassChoice(@Coerce Object msg, Supplier<NetworkEvent.Context> ctxSup,
            CallbackInfo ci) {
        NetworkEvent.Context ctx = ctxSup.get();
        if (ctx.getSender() != null) {
            ctx.enqueueWork(() -> {
                String className = net.bluelotuscoding.skillleveling.bridge.data.CustomClassData
                        .getCustomClass(ctx.getSender());

                if (className != null && !className.isEmpty()) {
                    net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.onClassChanged(ctx.getSender(), className);
                }
            });
        }
    }

}
