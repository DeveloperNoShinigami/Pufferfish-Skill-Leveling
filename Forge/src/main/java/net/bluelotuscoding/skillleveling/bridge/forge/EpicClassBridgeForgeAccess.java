package net.bluelotuscoding.skillleveling.bridge.forge;

import java.lang.reflect.Method;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

public final class EpicClassBridgeForgeAccess {
    private static final String CLASS_NAME = "com.example.epicclassmod.data.PlayerClassData";
    private static Method getMethod;
    private static boolean initialized = false;

    private EpicClassBridgeForgeAccess() {
    }

    public static boolean isAvailable(Object player) {
        if (initialized) {
            return getMethod != null;
        }

        initialized = true;
        if (player == null) {
            return false;
        }
        try {
            Class<?> playerClassData = Class.forName(CLASS_NAME);
            getMethod = findGetMethod(playerClassData, player);
        } catch (Exception e) {
            logWarn("Epic Class Mod access failed: " + e.getMessage());
            getMethod = null;
        }

        return getMethod != null;
    }

    public static String getClassName(Object player) {
        if (!isAvailable(player)) {
            return null;
        }

        try {
            Object result = getMethod.invoke(null, player);
            if (result instanceof Enum) {
                return ((Enum<?>) result).name();
            }
        } catch (Exception e) {
            logWarn("Failed to read Epic Class type: " + e.getMessage());
        }

        return null;
    }

    public static boolean isPlayer(Object player) {
        if (player == null) {
            return false;
        }
        String className = player.getClass().getName();
        return "net.minecraft.server.level.ServerPlayer".equals(className)
                || "net.minecraft.server.network.ServerPlayerEntity".equals(className)
                || "net.minecraft.client.player.LocalPlayer".equals(className)
                || "net.minecraft.client.network.ClientPlayerEntity".equals(className)
                || "net.minecraft.world.entity.player.Player".equals(className)
                || "net.minecraft.entity.player.PlayerEntity".equals(className);
    }

    public static boolean isServerPlayer(Object player) {
        if (player == null) {
            return false;
        }
        String className = player.getClass().getName();
        return "net.minecraft.server.level.ServerPlayer".equals(className)
                || "net.minecraft.server.network.ServerPlayerEntity".equals(className);
    }

    private static Method findGetMethod(Class<?> playerClassData, Object player) {
        for (Method method : playerClassData.getMethods()) {
            if (!"get".equals(method.getName())) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (!paramType.isAssignableFrom(player.getClass())) {
                continue;
            }
            return method;
        }
        return null;
    }

    public static Integer invokeExperienceInt(Object experience, String methodName, Object player) {
        try {
            for (Method method : experience.getClass().getMethods()) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                if (method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> paramType = method.getParameterTypes()[0];
                if (!paramType.isAssignableFrom(player.getClass())) {
                    continue;
                }
                Object result = method.invoke(experience, player);
                if (result instanceof Integer) {
                    return (Integer) result;
                }
            }
        } catch (Exception e) {
            SkillLevelingMod.getInstance().getLogger().debug(
                    "[Bridge] Reflection error calling Experience." + methodName + ": " + e.getMessage());
        }
        return null;
    }

    private static void logWarn(String message) {
        SkillLevelingMod.getInstance().getLogger().warn(message);
    }
}
