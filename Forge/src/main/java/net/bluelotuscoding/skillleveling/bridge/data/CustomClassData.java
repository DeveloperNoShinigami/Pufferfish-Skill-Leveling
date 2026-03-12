package net.bluelotuscoding.skillleveling.bridge.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import com.example.epicclassmod.data.PlayerClassData;

/**
 * Utility class to manage custom string-based class IDs,
 * bypassing the hardcoded Epic Class Enum.
 */
public final class CustomClassData {
    private static final String KEY_CLASS_NAME = "ecm_class_name"; // Epic Class's native key
    private static final String KEY_CHOSEN = "ecm_class_chosen";

    private CustomClassData() {
    }

    /**
     * Gets the custom string class ID from the player's NBT.
     * Defaults to "epic_classes:none" if not set.
     */
    public static String getCustomClass(PlayerEntity player) {
        if ((Object) player instanceof net.minecraftforge.common.extensions.IForgeEntity forgeEntity) {
            NbtCompound tag = forgeEntity.getPersistentData();
            String name = tag.getString(KEY_CLASS_NAME);

            if (name == null || name.isEmpty()) {
                return "epic_classes:none";
            }

            // Normalization: Lowercase everything
            String normalized = name.toLowerCase(java.util.Locale.ROOT);

            // If it's a legacy enum name (e.g., "SORCERER"), it won't have a colon
            if (!normalized.contains(":")) {
                return "epic_classes:" + normalized;
            }

            return normalized;
        }
        return "epic_classes:none";
    }

    /**
     * Sets the custom string class ID directly into the player's persistent data.
     */
    public static void setCustomClass(PlayerEntity player, String classId) {
        if ((Object) player instanceof net.minecraftforge.common.extensions.IForgeEntity forgeEntity) {
            NbtCompound tag = forgeEntity.getPersistentData();
            tag.putString(KEY_CLASS_NAME, classId);

            // Normalize for comparison
            String normalized = classId != null ? classId.toLowerCase(java.util.Locale.ROOT) : "epic_classes:none";
            boolean isChosen = !normalized.equals("epic_classes:none") && !normalized.equals("none");

            tag.putBoolean(KEY_CHOSEN, isChosen);
        }
    }

    /**
     * Fallback mapping from string ID to the nearest native ClassType enum,
     * used only when extremely deep/unreachable native EpicClass logic absolutely
     * demands an enum.
     * (We should strive to never use this if possible).
     */
    public static com.example.epicclassmod.data.PlayerClassData.ClassType getFallbackEnum(String classId) {
        if (classId == null || classId.isEmpty() || classId.equals("epic_classes:none")) {
            return com.example.epicclassmod.data.PlayerClassData.ClassType.NONE;
        }

        // 1. Try mapping the literal string to enum (e.g. "epic_classes:warrior" ->
        // WARRIOR)
        String idPart = classId.contains(":") ? classId.split(":")[1] : classId;
        try {
            return com.example.epicclassmod.data.PlayerClassData.ClassType.valueOf(idPart.toUpperCase());
        } catch (Exception ignored) {
        }

        // 2. Check for epic_class_proxy in the class definition
        net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef def = net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager
                .getClassDef(classId);

        if (def == null && classId.contains(":")) {
            // Fallback to searching without namespace (many configs omit 'epic_classes:'
            // prefix in keys)
            def = net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager.getClassDef(idPart);
        }

        if (def != null && def.epic_class_proxy != null && !def.epic_class_proxy.isEmpty()) {
            try {
                return com.example.epicclassmod.data.PlayerClassData.ClassType
                        .valueOf(def.epic_class_proxy.toUpperCase());
            } catch (Exception ignored) {
            }
        }

        // 3. Absolute fallback: NONE
        return com.example.epicclassmod.data.PlayerClassData.ClassType.NONE;
    }
}
