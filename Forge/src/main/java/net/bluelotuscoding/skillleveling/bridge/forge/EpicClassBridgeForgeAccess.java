package net.bluelotuscoding.skillleveling.bridge.forge;

import java.lang.reflect.Method;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.server.network.ServerPlayerEntity;

public final class EpicClassBridgeForgeAccess {
    private static final String CLASS_NAME = "com.example.epicclassmod.data.PlayerClassData";
    private static Method getMethod;
    private static boolean initialized = false;

    private EpicClassBridgeForgeAccess() {
    }

    public static boolean isAvailable(ServerPlayerEntity player) {
        if (initialized) {
            return getMethod != null;
        }

        initialized = true;
        try {
            Class<?> playerClassData = Class.forName(CLASS_NAME);
            getMethod = findGetMethod(playerClassData, player);
        } catch (Exception e) {
            logWarn("Epic Class Mod access failed: " + e.getMessage());
            getMethod = null;
        }

        return getMethod != null;
    }

    public static String getClassName(ServerPlayerEntity player) {
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

    private static Method findGetMethod(Class<?> playerClassData, ServerPlayerEntity player) {
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

    private static void logWarn(String message) {
        SkillLevelingMod.getInstance().getLogger().warn(message);
    }
}
