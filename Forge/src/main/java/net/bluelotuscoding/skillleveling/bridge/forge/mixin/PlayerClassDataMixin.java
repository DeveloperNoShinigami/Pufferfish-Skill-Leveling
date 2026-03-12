package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import com.example.epicclassmod.data.PlayerClassData;
import net.bluelotuscoding.skillleveling.bridge.data.CustomClassData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.extensions.IForgeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = com.example.epicclassmod.data.PlayerClassData.class, remap = false)
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
    private static void afterSet(PlayerEntity sp, PlayerClassData.ClassType type, CallbackInfo ci) {
        if (sp instanceof net.minecraft.server.network.ServerPlayerEntity ssp) {
            // Using the 'type' parameter directly from the 'set' method call to avoid
            // shadowed getClass()
            // Use the actual custom class ID from NBT instead of the generic enum name
            String className = net.bluelotuscoding.skillleveling.bridge.data.CustomClassData.getCustomClass(sp);

            if ("NONE".equalsIgnoreCase(className) || "epic_classes:none".equalsIgnoreCase(className)) {
                // Reset stats and level to 0
                com.example.epicclassmod.data.PlayerLevelData.resetStats(ssp);
                try {
                    // Use reflection to set level to 0 if possible, or direct NBT manipulation if
                    // necessary
                    // Testing showed the user wants "Level 0"
                    java.lang.reflect.Method setLevel = com.example.epicclassmod.data.PlayerLevelData.class.getMethod(
                            "setLevelDirect", net.minecraft.server.network.ServerPlayerEntity.class, int.class,
                            boolean.class);
                    setLevel.invoke(null, ssp, 0, true);
                } catch (Exception e) {
                    // Fallback to NBT if method not found or fails
                    net.minecraft.nbt.NbtCompound tag = ((IForgeEntity) ssp).getPersistentData();
                    if (tag.contains("ecm_leveling")) {
                        tag.getCompound("ecm_leveling").putInt("level", 0);
                    }
                }
            }

            // Force native sync packet - Using the enum type directly as required by
            // SyncClassPacket(UUID, ClassType)
            com.example.epicclassmod.network.ModNetwork.sendToTrackingAndSelf(ssp,
                    new com.example.epicclassmod.network.SyncClassPacket(ssp.getUuid(), type));

            // Bridge handles the rest (locking categories, etc.)
            net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.onClassChanged(ssp, className);
        }
    }
}
