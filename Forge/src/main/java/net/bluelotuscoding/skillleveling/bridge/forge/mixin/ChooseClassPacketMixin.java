package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import com.example.epicclassmod.network.ChooseClassPacket;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.function.Supplier;

@Mixin(value = ChooseClassPacket.class, remap = false)
public abstract class ChooseClassPacketMixin {

    /**
     * @author Antigravity
     * @reason Ensure base classes getting chosen via native packet still trigger
     *         the skill level reset logic.
     */
    @Inject(method = "handle", at = @At("TAIL"))
    private static void onHandleClassChoice(ChooseClassPacket msg, Supplier<NetworkEvent.Context> ctxSup,
            CallbackInfo ci) {
        NetworkEvent.Context ctx = ctxSup.get();
        if (ctx.getSender() != null) {
            ctx.enqueueWork(() -> {
                // By this time the class is set via PlayerClassData.
                String className = net.bluelotuscoding.skillleveling.bridge.forge.EpicClassBridgeForgeAccess
                        .getClassName(ctx.getSender());

                if (className != null && !className.isEmpty()) {
                    EpicClassBridge.onClassChanged(ctx.getSender(), className);

                }
            });
        }
    }

}
