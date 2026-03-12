package net.bluelotuscoding.skillleveling.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Keybind for toggling item restriction tooltip visibility.
 * Default: Left Alt (hold to reveal restrictions on items).
 * Can be rebound in Controls settings.
 */
public class ItemRestrictionKeybind {
    public static final String CATEGORY = "category.skillleveling.mastery";

    public static final KeyBinding VIEW_RESTRICTIONS = new KeyBinding(
            "key.skillleveling.view_restrictions",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            CATEGORY);

    /**
     * Returns true while the keybind is held down.
     * Uses raw GLFW key state instead of KeyBinding.isPressed() which gets
     * consumed.
     */
    public static boolean isShowingRestrictions() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.getWindow() == null) {
                return false;
            }
            long handle = mc.getWindow().getHandle();
            InputUtil.Key boundKey = getBoundKey();
            return InputUtil.isKeyPressed(handle, boundKey.getCode());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the InputUtil.Key that is currently bound to this keybind.
     */
    private static InputUtil.Key getBoundKey() {
        return ((net.bluelotuscoding.skillleveling.mixin.KeyBindingAccessor) VIEW_RESTRICTIONS).getBoundKey();
    }

    /**
     * Returns the display name of the currently bound key (e.g. "Left Alt", "R",
     * "Left Control").
     */
    public static String getKeyName() {
        return VIEW_RESTRICTIONS.getBoundKeyLocalizedText().getString();
    }
}
