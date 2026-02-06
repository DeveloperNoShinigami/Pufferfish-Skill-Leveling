package net.bluelotuscoding.skillleveling.client;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

/**
 * SIDE-SAFE CLIENT UTILITY
 * 
 * Provides reflected access to MinecraftClient to avoid NoClassDefFoundError
 * and MixinTransformerError on server threads, especially in common packets.
 */
public class SideSafeClient {

    private static Class<?> mcClass;
    private static Method getInstanceMethod;
    private static Field currentScreenField;
    private static Method setScreenMethod;
    private static Field playerField;

    static {
        try {
            mcClass = Class.forName("net.minecraft.client.MinecraftClient");
            getInstanceMethod = mcClass.getMethod("getInstance");
            currentScreenField = mcClass.getField("currentScreen");
            setScreenMethod = mcClass.getMethod("setScreen", Class.forName("net.minecraft.client.gui.screen.Screen"));
            playerField = mcClass.getField("player");
        } catch (Exception ignored) {
            // Probably on a dedicated server or class not yet available
        }
    }

    public static Object getMinecraft() {
        if (getInstanceMethod == null)
            return null;
        try {
            return getInstanceMethod.invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static Object getPlayer() {
        Object mc = getMinecraft();
        if (mc == null || playerField == null)
            return null;
        try {
            return playerField.get(mc);
        } catch (Exception e) {
            return null;
        }
    }

    public static void closeScreen() {
        Object mc = getMinecraft();
        if (mc == null || setScreenMethod == null)
            return;
        try {
            // check if current screen is not null
            Object current = currentScreenField.get(mc);
            if (current != null) {
                setScreenMethod.invoke(mc, (Object) null);
            }
        } catch (Exception e) {
            // Log if needed, but usually we just want to fail gracefully if client isn't
            // there
        }
    }
}
