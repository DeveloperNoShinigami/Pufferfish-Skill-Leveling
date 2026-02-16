package net.bluelotuscoding.skillleveling.client;

/**
 * SIDE-SAFE CLIENT UTILITY
 * 
 * Provides safe access to MinecraftClient methods without causing
 * NoClassDefFoundError on dedicated servers. Uses inner classes so that
 * MinecraftClient is only resolved when actually invoked (client-side only).
 * 
 * Direct class references (not Class.forName strings) ensure the build system
 * correctly remaps class names for production environments (SRG/Mojmap).
 */
public class SideSafeClient {

    public static Object getMinecraft() {
        try {
            return ClientAccess.getMinecraft();
        } catch (NoClassDefFoundError e) {
            return null;
        }
    }

    public static Object getPlayer() {
        try {
            return ClientAccess.getPlayer();
        } catch (NoClassDefFoundError e) {
            return null;
        }
    }

    public static void closeScreen() {
        try {
            ClientAccess.closeScreen();
        } catch (NoClassDefFoundError e) {
            // Not on client side — should never happen since this is only called
            // from client packet handlers
        }
    }

    /**
     * Inner class that holds all direct MinecraftClient references.
     * Only loaded by the JVM when one of its methods is first called,
     * ensuring dedicated servers never trigger class resolution.
     */
    private static class ClientAccess {
        static Object getMinecraft() {
            return net.minecraft.client.MinecraftClient.getInstance();
        }

        static Object getPlayer() {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            return mc != null ? mc.player : null;
        }

        static void closeScreen() {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc != null && mc.currentScreen != null) {
                mc.execute(() -> mc.setScreen(null));
            }
        }
    }
}
