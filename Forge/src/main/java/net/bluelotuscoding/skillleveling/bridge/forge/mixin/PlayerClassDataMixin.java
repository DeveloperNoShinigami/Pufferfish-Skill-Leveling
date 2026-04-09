package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.entity.player.PlayerEntity;

@Pseudo
@Mixin(targets = "com.example.epicclassmod.data.PlayerClassData", remap = false)
public class PlayerClassDataMixin {

    /**
     * Intercept the native ClassType fetch to pull from our custom String
     * ID NBT, mapping back to the enum if necessary.
     *
     * Uses @Inject + cancellation instead of @Overwrite for resilience
     * against minor epicclassmod version changes.
     */
    @SuppressWarnings("rawtypes")
    @Inject(method = "get", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGet(PlayerEntity p, CallbackInfoReturnable<Object> cir) {
        try {
            // Only intercept on the server side — on the client, let native
            // epicclassmod logic handle it to avoid freezes from unsynced data.
            if (p.getWorld().isClient()) {
                return;
            }
            String customId = net.bluelotuscoding.skillleveling.bridge.data.CustomClassData.getCustomClass(p);
            Object fallback = net.bluelotuscoding.skillleveling.bridge.data.CustomClassData.getFallbackEnum(customId);
            cir.setReturnValue(fallback);
        } catch (Exception e) {
            // If our bridge fails, fall through to native epicclassmod logic
        }
    }

    @Inject(method = "set", at = @At("HEAD"), remap = false)
    private static void beforeSet(PlayerEntity player, @Coerce Object type, CallbackInfo ci) {
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity ssp)) {
            return;
        }

        if (addon$isResetType(type)) {
            net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getSkillLevelingManager()
                    .resetPufferfishProgressForClassReset(ssp);
        }
    }

    @Inject(method = "set", at = @At("TAIL"), remap = false)
    private static void afterSet(PlayerEntity player, @Coerce Object type, CallbackInfo ci) {
        // Safe-cast: only process if it's a ServerPlayerEntity on the server side
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity ssp)) {
            return;
        }

        String className = net.bluelotuscoding.skillleveling.bridge.data.CustomClassData.getCustomClass(ssp);

        if ("NONE".equalsIgnoreCase(className) || "epic_classes:none".equalsIgnoreCase(className)) {
            try {
                Class<?> pldClass = Class.forName("com.example.epicclassmod.data.PlayerLevelData");
                java.lang.reflect.Method resetStats = pldClass.getMethod("resetStats", player.getClass());
                resetStats.invoke(null, player);

                java.lang.reflect.Method setLevel = pldClass.getMethod("setLevelDirect", player.getClass(), int.class,
                        boolean.class);
                setLevel.invoke(null, player, 0, true);

            } catch (Exception e) {
                net.minecraft.nbt.NbtCompound tag = ((net.minecraftforge.common.extensions.IForgeEntity) ssp)
                        .getPersistentData();
                if (tag.contains("ecm_leveling")) {
                    tag.getCompound("ecm_leveling").putInt("level", 0);
                }
            }
        }

        try {
            Class<?> modNetworkClass = Class.forName("com.example.epicclassmod.network.ModNetwork");
            Class<?> syncPacketClass = Class.forName("com.example.epicclassmod.network.SyncClassPacket");
            java.lang.reflect.Constructor<?> syncPacketCtor = syncPacketClass.getConstructor(java.util.UUID.class,
                    type.getClass());
            Object syncPacket = syncPacketCtor.newInstance(ssp.getUuid(), type);

            java.lang.reflect.Method sendTracking = modNetworkClass.getMethod("sendToTrackingAndSelf", ssp.getClass(),
                    Object.class);
            sendTracking.invoke(null, ssp, syncPacket);
        } catch (Exception e) {
            // Ignore if mod network fails
        }

        net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.onClassChanged(ssp, className);
    }

    private static boolean addon$isResetType(Object type) {
        if (type == null) {
            return false;
        }

        try {
            if ("NONE".equalsIgnoreCase(type.toString()) || "epic_classes:none".equalsIgnoreCase(type.toString())) {
                return true;
            }

            try {
                var nameMethod = type.getClass().getMethod("name");
                Object name = nameMethod.invoke(type);
                if (name != null && "NONE".equalsIgnoreCase(name.toString())) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }

        return false;
    }
}
