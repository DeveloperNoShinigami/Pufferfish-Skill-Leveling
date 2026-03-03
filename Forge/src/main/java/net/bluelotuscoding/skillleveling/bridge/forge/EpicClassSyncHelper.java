package net.bluelotuscoding.skillleveling.bridge.forge;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.bridge.config.ClassPageDef;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import java.lang.reflect.Method;
import net.bluelotuscoding.skillleveling.bridge.network.SyncCustomNbtPacket;
import net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Helper to sync between Pufferfish and Epic Classes.
 * Treating Pufferfish as the Master Source of Truth for XP and Levels.
 */
public class EpicClassSyncHelper {

    private static final String PLAYER_LEVEL_DATA_CLASS = "com.example.epicclassmod.data.PlayerLevelData";
    private static final String MOD_NETWORK_CLASS = "com.example.epicclassmod.network.ModNetwork";

    private static Class<?> playerLevelDataClass;
    private static Class<?> modNetworkClass;
    private static Class<?> syncLevelPacketClass;

    private static Method rootMethod;
    private static Method applyAllModifiersMethod;

    private static boolean reflectionFailed = false;

    // Loop Guard: Prevent recursion when mirroring state back and forth
    private static final ThreadLocal<Boolean> IS_SYNCING = ThreadLocal.withInitial(() -> false);

    public static boolean isSyncing() {
        return IS_SYNCING.get();
    }

    private static void initReflection() {
        if (playerLevelDataClass != null || reflectionFailed) {
            return;
        }

        try {
            playerLevelDataClass = Class.forName(PLAYER_LEVEL_DATA_CLASS);
            modNetworkClass = Class.forName(MOD_NETWORK_CLASS);
            syncLevelPacketClass = Class.forName("com.example.epicclassmod.network.SyncLevelPacket");

            for (Method m : playerLevelDataClass.getDeclaredMethods()) {
                if (m.getName().equals("root") && m.getParameterCount() == 1) {
                    rootMethod = m;
                    rootMethod.setAccessible(true);
                } else if (m.getName().equals("applyAllModifiers") && m.getParameterCount() == 1) {
                    applyAllModifiersMethod = m;
                    applyAllModifiersMethod.setAccessible(true);
                }
            }

            if (rootMethod == null || applyAllModifiersMethod == null) {
                throw new RuntimeException("Could not find necessary Epic Class methods");
            }

        } catch (Exception e) {
            AddonLogger.LOGGER.error("[Bridge] Failed to reflect Epic Class internals: " + e.getMessage());
            reflectionFailed = true;
        }
    }

    public static void forceSync(Object player) {
        forceSync(player, 0);
    }

    public static void forceSync(Object player, int lastGain) {
        initReflection();
        if (reflectionFailed) {
            return;
        }

        try {
            Method ofMethod = null;
            for (Method m : syncLevelPacketClass.getDeclaredMethods()) {
                if (m.getName().equals("of") && m.getParameterCount() == 2 && m.getParameterTypes()[1] == int.class) {
                    ofMethod = m;
                    break;
                }
            }

            if (ofMethod != null) {
                Object packet = ofMethod.invoke(null, player, lastGain);
                Method sendToMethod = null;
                for (Method m : modNetworkClass.getDeclaredMethods()) {
                    if (m.getName().equals("sendTo") && m.getParameterCount() == 2) {
                        sendToMethod = m;
                        break;
                    }
                }
                if (sendToMethod != null) {
                    sendToMethod.invoke(null, player, packet);
                }
            }

            // Sync our custom NBT tag as well
            if (player instanceof ServerPlayerEntity sp) {
                Object res = rootMethod.invoke(null, sp);
                if (res instanceof NbtCompound nbt) {
                    ForgeNetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                            new SyncCustomNbtPacket(nbt));
                }
            }
        } catch (Exception e) {
            AddonLogger.LOGGER.error("[Bridge] Failed to force sync: " + e.getMessage());
        }
    }

    public static void applyModifiers(Object player) {
        initReflection();
        if (reflectionFailed) {
            return;
        }
        try {
            applyAllModifiersMethod.invoke(null, player);
            // After standard modifiers, apply our custom JSON ones
            if (player instanceof ServerPlayerEntity sp) {
                applyCustomAttributes(sp);
            }
        } catch (Exception e) {
            AddonLogger.LOGGER.error("[Bridge] Failed to apply modifiers: " + e.getMessage());
        }
    }

    private static final UUID UUID_ADDON_BASE = UUID.fromString("e3d5b1a0-7f2a-4c2b-8a1a-9c2b3d4e5f60");

    public static void applyCustomAttributes(ServerPlayerEntity sp) {
        String className = net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getPlatform()
                .getEpicClassName(sp);
        var def = EpicClassConfigManager.getClassDef(className);
        if (def == null) {
            return;
        }

        List<ClassPageDef> pages = EpicClassConfigManager.getPagesForClass(def.class_name);
        Set<String> validIds = new HashSet<>();

        for (var page : pages) {
            for (var attr : page.slots) {
                if (attr.id != null) {
                    validIds.add(attr.id);
                }
            }
        }

        // 1. Cleanup old modifiers that are no longer in the config
        cleanupOldModifiers(sp, validIds);

        // 2. Apply current modifiers
        for (var page : pages) {
            for (var attr : page.slots) {
                if (attr.id == null || attr.attribute_id == null) {
                    continue;
                }

                int points = getStatPoints(sp, attr.id);
                double value = 0;

                if (attr.compiledExpression != null) {
                    try {
                        value = attr.compiledExpression.eval(Map.of("points", (double) points));
                    } catch (Exception e) {
                        AddonLogger.LOGGER
                                .warn("[Bridge] Failed to evaluate expression for " + attr.id + ": " + e.getMessage());
                    }
                }

                applyModifierSafely(sp, attr.attribute_id, value, attr.id, attr.operation);
            }
        }
        // Force attribute sync to client so inst.getValue() reflects new values
        syncAttributesToClient(sp);
    }

    /**
     * Force-syncs all tracked attribute instances to the client.
     * This ensures the client sees updated modifier values after allocation/reset.
     */
    public static void syncAttributesToClient(ServerPlayerEntity sp) {
        try {
            // Mark all tracked instances as dirty so Minecraft sends them
            for (var inst : sp.getAttributes().getTracked()) {
                // Accessing the value re-computes and marks dirty if changed
                inst.getValue();
            }
            // Use Minecraft's built-in attribute sync mechanism
            var dirtyAttributes = sp.getAttributes().getTracked().stream()
                    .filter(inst -> true) // force all
                    .toList();
            if (!dirtyAttributes.isEmpty()) {
                var packet = new net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket(
                        sp.getId(), dirtyAttributes);
                sp.networkHandler.sendPacket(packet);
            }
        } catch (Exception e) {
            AddonLogger.LOGGER.error("[Bridge] Failed to sync attributes: " + e.getMessage());
        }
    }

    private static void cleanupOldModifiers(ServerPlayerEntity sp, Set<String> validIds) {
        if (sp.getAttributes() == null) {
            return;
        }
        for (EntityAttributeInstance inst : sp.getAttributes().getTracked()) {
            List<EntityAttributeModifier> toRemove = new ArrayList<>();
            for (EntityAttributeModifier mod : inst.getModifiers()) {
                if (mod.getName().startsWith("ecm_addon_")) {
                    String id = mod.getName().substring("ecm_addon_".length());
                    if (!validIds.contains(id)) {
                        toRemove.add(mod);
                    }
                }
            }
            for (EntityAttributeModifier mod : toRemove) {
                inst.removeModifier(mod);
            }
        }
    }

    private static void applyModifierSafely(ServerPlayerEntity sp, String attrId, double value,
            String slotId, String opStr) {
        try {
            EntityAttribute attr = ForgeRegistries.ATTRIBUTES
                    .getValue(new Identifier(attrId));
            if (attr == null) {
                return;
            }

            EntityAttributeInstance inst = sp.getAttributeInstance(attr);
            if (inst == null) {
                return;
            }

            // Generate a deterministic UUID based on the slot ID
            UUID modUuid = new UUID(UUID_ADDON_BASE.getMostSignificantBits(), slotId.hashCode());

            EntityAttributeModifier old = inst.getModifier(modUuid);
            if (old != null) {
                inst.removeModifier(modUuid);
            }

            EntityAttributeModifier.Operation op = EntityAttributeModifier.Operation.ADDITION;
            if (opStr != null) {
                op = switch (opStr.toUpperCase()) {
                    case "MULTIPLY_BASE" ->
                        EntityAttributeModifier.Operation.MULTIPLY_BASE;
                    case "MULTIPLY_TOTAL" ->
                        EntityAttributeModifier.Operation.MULTIPLY_TOTAL;
                    default -> EntityAttributeModifier.Operation.ADDITION;
                };
            }

            if (value != 0) {
                inst.addTemporaryModifier(new EntityAttributeModifier(modUuid, "ecm_addon_" + slotId,
                        value, op));
            }
        } catch (Exception e) {
            AddonLogger.LOGGER.error("[Bridge] Failed to apply custom modifier " + slotId + ": " + e.getMessage());
        }
    }

    /**
     * Master Sync: Sets ECM data from Pufferfish values.
     * Called whenever Pufferfish XP or Level changes.
     */
    public static void syncFromPufferfish(PlayerEntity player, int newLevel, int newXp, int lastGain) {
        if (isSyncing()) {
            return;
        }

        initReflection();
        if (reflectionFailed) {
            return;
        }

        try {
            IS_SYNCING.set(true);
            Object result = rootMethod.invoke(null, player);
            if (result != null) {
                // Use reflection for NBT access to be safe across all mapping environments
                Method getInt = null;
                Method putInt = null;
                try {
                    getInt = result.getClass().getMethod("m_128451_", String.class);
                    putInt = result.getClass().getMethod("m_128405_", String.class, int.class);
                } catch (NoSuchMethodException e) {
                    try {
                        getInt = result.getClass().getMethod("getInt", String.class);
                        putInt = result.getClass().getMethod("putInt", String.class, int.class);
                    } catch (NoSuchMethodException e2) {
                        // Fail
                    }
                }

                if (getInt != null && putInt != null) {
                    int used = 0;
                    if (result instanceof net.minecraft.nbt.NbtCompound tag) {
                        for (String key : tag.getKeys()) {
                            if (key.startsWith("alloc_")) {
                                used += tag.getInt(key);
                            }
                        }
                    }

                    net.bluelotuscoding.skillleveling.util.Platform platform = net.bluelotuscoding.skillleveling.SkillLevelingMod
                            .getInstance().getPlatform();

                    int authoritativeLevel = newLevel;
                    int authoritativeXp = newXp;
                    int spNow = 0;

                    java.util.Optional<net.minecraft.util.Identifier> catIdOpt = EpicClassBridge
                            .getActiveCategory(player);

                    if (catIdOpt.isPresent()) {
                        authoritativeLevel = platform.getPufferfishLevel(player, catIdOpt.get());
                        authoritativeXp = platform.getPufferfishExperience(player, catIdOpt.get());
                        spNow = Math.max(0, authoritativeLevel - used);
                    } else {
                        // Fallback to arguments if no class category is active
                        spNow = Math.max(0, newLevel - used);
                    }

                    putInt.invoke(result, "level", authoritativeLevel);
                    putInt.invoke(result, "xp", authoritativeXp);
                    putInt.invoke(result, "stat_points", spNow);

                    // Ensure the tag is actually attached to the player's persistent data
                    // In Case root() returned a new empty compound that wasn't previously there
                    net.minecraft.nbt.NbtCompound persistentData = player.getPersistentData();
                    if (!persistentData.contains("ecm_leveling")) {
                        persistentData.put("ecm_leveling", (net.minecraft.nbt.NbtElement) result);
                    }

                    applyModifiers(player);
                    forceSync(player, lastGain);

                    AddonLogger.LOGGER
                            .info("[Bridge] Synced Pufferfish -> ECM (Authoritative): Level=" + authoritativeLevel
                                    + ", SP=" + spNow + " (Used="
                                    + used + ")");
                } else {
                    AddonLogger.LOGGER.warn("[Bridge] Could not find NBT getInt/putInt methods via reflection");
                }
            }
        } catch (Exception e) {
            AddonLogger.LOGGER.warn("[Bridge] Error during Pufferfish -> ECM sync: " + e.getMessage());
        } finally {
            IS_SYNCING.set(false);
        }
    }

    public static int getStatPoints(PlayerEntity player, String statId) {
        initReflection();
        if (reflectionFailed || statId == null) {
            return 0;
        }
        try {
            Object result = rootMethod.invoke(null, player);
            if (result != null) {
                Method getInt = null;
                try {
                    getInt = result.getClass().getMethod("m_128451_", String.class);
                } catch (NoSuchMethodException e) {
                    try {
                        getInt = result.getClass().getMethod("getInt", String.class);
                    } catch (NoSuchMethodException e2) {
                    }
                }
                if (getInt != null) {
                    // Check for standard stat IDs
                    String tagKey = switch (statId) {
                        case "atk" -> "alloc_atk";
                        case "def" -> "alloc_def";
                        case "aspd" -> "alloc_aspd";
                        case "mspd" -> "alloc_mspd";
                        case "cooldown" -> "alloc_cooldown";
                        case "regen" -> "alloc_regen";
                        default -> statId.startsWith("alloc_") ? statId : "alloc_" + statId;
                    };
                    return (int) getInt.invoke(result, tagKey);
                }
            }
        } catch (Exception e) {
        }
        return 0;
    }

    public static int getAvailablePoints(PlayerEntity player) {
        initReflection();
        if (reflectionFailed) {
            return 0;
        }
        try {
            Object result = rootMethod.invoke(null, player);
            if (result != null) {
                Method getInt = null;
                try {
                    getInt = result.getClass().getMethod("m_128451_", String.class);
                } catch (NoSuchMethodException e) {
                    try {
                        getInt = result.getClass().getMethod("getInt", String.class);
                    } catch (NoSuchMethodException e2) {
                    }
                }
                if (getInt != null) {
                    return (int) getInt.invoke(result, "stat_points");
                }
            }
        } catch (Exception e) {
        }
        return 0;
    }

    public static void allocateStat(PlayerEntity player, String statId, int points) {
        initReflection();
        if (reflectionFailed || statId == null) {
            return;
        }
        try {
            Object result = rootMethod.invoke(null, player);
            if (result != null) {
                Method getInt = null;
                Method putInt = null;
                try {
                    getInt = result.getClass().getMethod("m_128451_", String.class);
                    putInt = result.getClass().getMethod("m_128405_", String.class, int.class);
                } catch (NoSuchMethodException e) {
                    try {
                        getInt = result.getClass().getMethod("getInt", String.class);
                        putInt = result.getClass().getMethod("putInt", String.class, int.class);
                    } catch (NoSuchMethodException e2) {
                    }
                }
                if (getInt != null && putInt != null) {
                    int available = (int) getInt.invoke(result, "stat_points");
                    if (available >= points) {
                        String tagKey = switch (statId) {
                            case "atk" -> "alloc_atk";
                            case "def" -> "alloc_def";
                            case "aspd" -> "alloc_aspd";
                            case "mspd" -> "alloc_mspd";
                            case "cooldown" -> "alloc_cooldown";
                            case "regen" -> "alloc_regen";
                            default -> statId.startsWith("alloc_") ? statId : "alloc_" + statId;
                        };
                        int current = (int) getInt.invoke(result, tagKey);
                        putInt.invoke(result, tagKey, current + points);
                        putInt.invoke(result, "stat_points", available - points);

                        applyModifiers(player);
                        forceSync(player, 0);
                    }
                }
            }
        } catch (Exception e) {
            AddonLogger.LOGGER.error("[Bridge] Failed to force sync: " + e.getMessage());
        }
    }
}
