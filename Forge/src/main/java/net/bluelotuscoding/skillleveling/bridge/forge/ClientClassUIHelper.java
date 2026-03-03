package net.bluelotuscoding.skillleveling.bridge.forge;

import net.minecraft.client.MinecraftClient;

/**
 * Helper to handle client-side UI updates for Epic Class Mod.
 * Centralizes the reflection logic to avoid Mixin visibility and interaction
 * issues.
 */
public class ClientClassUIHelper {

    private static final String STATE_CLASS = "com.example.epicclassmod.client.ClientClassState";

    /**
     * Updates the displayName and displayNameFallback fields in ClientClassState.
     */
    public static void updateDisplayNameFields(String name) {
        if (name == null) {
            return;
        }
        try {
            Class<?> targetClass = Class.forName(STATE_CLASS);

            java.lang.reflect.Field displayNameField = null;
            try {
                displayNameField = targetClass.getDeclaredField("displayName");
            } catch (NoSuchFieldException e) {
                displayNameField = targetClass.getDeclaredField("display_name");
            }
            displayNameField.setAccessible(true);
            displayNameField.set(null, name);

            java.lang.reflect.Field displayNameFallbackField = null;
            try {
                displayNameFallbackField = targetClass.getDeclaredField("displayNameFallback");
            } catch (NoSuchFieldException e) {
                displayNameFallbackField = targetClass.getDeclaredField("display_name_fallback");
            }
            displayNameFallbackField.setAccessible(true);
            displayNameFallbackField.set(null, name);
        } catch (Exception e) {
            // Silently fail or log if needed
        }
    }

    /**
     * Forces a refresh of the client-side class state fields.
     * Called from network packets when class data is synchronized.
     */
    public static void forceRefresh() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            try {
                String customId = net.bluelotuscoding.skillleveling.client.ClientCustomClassState
                        .getCustomClass(mc.player.getUuid());
                String name = null;

                if (customId != null && !"epic_classes:none".equals(customId)) {
                    var def = net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager
                            .getClassDef(customId);
                    if (def != null) {
                        name = (def.display_name != null && !def.display_name.isEmpty()) ? def.display_name
                                : def.class_name;
                    }
                }

                if (name == null) {
                    // Fallback to enum class name if no custom class
                    Class<?> targetClass = Class.forName(STATE_CLASS);
                    java.lang.reflect.Field selectedTypeField = targetClass.getDeclaredField("selectedType");
                    selectedTypeField.setAccessible(true);
                    Object selectedTypeObj = selectedTypeField.get(null);
                    if (selectedTypeObj instanceof Enum) {
                        Enum<?> selectedType = (Enum<?>) selectedTypeObj;
                        if (!"NONE".equals(selectedType.name())) {
                            String classNameStr = "epic_classes:" + selectedType.name().toLowerCase();
                            var def = net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager
                                    .getClassDef(classNameStr);
                            if (def != null) {
                                name = (def.display_name != null && !def.display_name.isEmpty()) ? def.display_name
                                        : def.class_name;
                            } else {
                                // Last resort: just the enum name capitalized
                                name = selectedType.name().charAt(0) + selectedType.name().substring(1).toLowerCase();
                            }
                        }
                    }
                }

                if (name != null) {
                    updateDisplayNameFields(name);
                }
            } catch (Exception e) {
            }
        }
    }
}
