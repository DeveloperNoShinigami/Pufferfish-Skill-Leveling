package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import com.example.epicclassmod.data.PlayerClassData;
import net.bluelotuscoding.skillleveling.bridge.data.CustomClassData;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(targets = "com.example.epicclassmod.data.PlayerClassData", remap = false)
public class PlayerClassDataMixin {

    /**
     * @author Antigravity
     * @reason Overwriting the native ClassType fetch to pull from our custom String
     *         ID NBT, mapping back if necessary.
     */
    @Overwrite
    public static PlayerClassData.ClassType get(PlayerEntity p) {
        String customId = CustomClassData.getCustomClass(p);
        return CustomClassData.getFallbackEnum(customId);
    }

    @Inject(method = "set", at = @At("TAIL"), remap = false)
    private static void afterSet(PlayerEntity p, PlayerClassData.ClassType t,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        // 1. Store our extended string ID in NBT (keeping sync with whatever Epic Class
        // just set)
        String customId = (t == null || t == PlayerClassData.ClassType.NONE)
                ? "epic_classes:none"
                : "epic_classes:" + t.name().toLowerCase();
        CustomClassData.setCustomClass(p, customId);

        // 2. Trigger Pufferfish category sync/lock for this class
        if (p instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
            String idToSync = (t == null) ? "NONE" : t.name();
            net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.onClassChanged(sp, idToSync);
        }
    }
}
