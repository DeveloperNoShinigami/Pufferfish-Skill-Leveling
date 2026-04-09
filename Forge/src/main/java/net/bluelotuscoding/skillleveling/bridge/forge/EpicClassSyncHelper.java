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
        } catch (Exception e) {
            AddonLogger.LOGGER.error("[Bridge] Failed to apply standard modifiers: " + e.getMessage());
        }

        try {
            // After standard modifiers, apply our custom JSON ones
            if (player instanceof ServerPlayerEntity sp) {
                applyCustomAttributes(sp);
            }
        } catch (Exception e) {
            AddonLogger.LOGGER.error("[Bridge] Failed to apply custom modifiers: " + e.getMessage());
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

                // Treat plain numeric values as per-point scalars for spendable stat slots.
                // Example: value="1" means +1 per point, not a constant +1 at zero points.
                if (attr.value != null) {
                    String expr = attr.value.trim();
                    if (!expr.isEmpty() && !expr.contains("points")) {
                        try {
                            value = Double.parseDouble(expr) * points;
                        } catch (NumberFormatException ignored) {
                            // Keep evaluated expression value if this is not a plain number.
                        }
                    }
                }

                if (points <= 0) {
                    value = 0;
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
    public static void syncFromPufferfish(PlayerEntity player, int newLevel, int newXp, int neededXp, int lastGain) {
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
                    net.minecraft.nbt.NbtCompound ecmNbt = (result instanceof net.minecraft.nbt.NbtCompound t) ? t : null;
                    if (ecmNbt != null) {
                        for (String key : ecmNbt.getKeys()) {
                            if (key.startsWith("alloc_")) {
                                used += ecmNbt.getInt(key);
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
                    }

                    // 4-way SPL resolution: NBT int → NBT string → per-class def → global config
                    String splKey = "stat_points_per_level";
                    int resolvedSPL;
                    if (ecmNbt != null && ecmNbt.contains(splKey, 3)) {
                        resolvedSPL = ecmNbt.getInt(splKey);
                    } else if (ecmNbt != null && ecmNbt.contains(splKey, 8)) {
                        String splExpr = ecmNbt.getString(splKey);
                        var splParseResult = net.puffish.skillsmod.expression.DefaultParser.parse(splExpr, java.util.Set.of("level"));
                        int evalLevel = authoritativeLevel;
                        resolvedSPL = splParseResult.getSuccess().map(expr -> {
                            try { return (int) Math.ceil(expr.eval(java.util.Map.of("level", (double) evalLevel))); }
                            catch (Exception ex) { return 1; }
                        }).orElse(1);
                    } else {
                        String ecmClassName = (player instanceof net.minecraft.server.network.ServerPlayerEntity sp2)
                                ? platform.getEpicClassName(sp2) : null;
                        var classDef = EpicClassConfigManager.getClassDef(ecmClassName);
                        int classDefSPL = (classDef != null) ? classDef.stat_points_per_level : 0;
                        resolvedSPL = (classDefSPL > 0) ? classDefSPL
                                : EpicClassConfigManager.getSyncedConfig().stat_points_per_level;
                    }

                    if (resolvedSPL > 0) {
                        spNow = Math.max(0, authoritativeLevel * resolvedSPL - used);
                    }

                    putInt.invoke(result, "level", authoritativeLevel);
                    putInt.invoke(result, "xp", authoritativeXp);
                    putInt.invoke(result, "xp_needed", neededXp); // Pass needed XP for client-side scaling
                    if (resolvedSPL > 0) {
                        putInt.invoke(result, "stat_points", spNow);
                    }

                    // Ensure the tag is actually attached to the player's persistent data
                    // In Case root() returned a new empty compound that wasn't previously there
                    net.minecraft.nbt.NbtCompound persistentData = player.getPersistentData();
                    if (!persistentData.contains("ecm_leveling")) {
                        persistentData.put("ecm_leveling", (net.minecraft.nbt.NbtElement) result);
                    }

                    applyModifiers(player);
                    forceSync(player, lastGain);

                    AddonLogger.LOGGER.info(
                            "[Bridge] Synced Pufferfish -> ECM (Authoritative): Level=" + authoritativeLevel
                                    + ", XP=" + authoritativeXp + "/" + neededXp + ", SP=" + spNow + ", Used=" + used);
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
        if (reflectionFailed || statId == null || points == 0) {
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
                    String tagKey = switch (statId) {
                        case "atk" -> "alloc_atk";
                        case "def" -> "alloc_def";
                        case "aspd" -> "alloc_aspd";
                        case "mspd" -> "alloc_mspd";
                        case "cooldown" -> "alloc_cooldown";
                        case "regen" -> "alloc_regen";
                        default -> statId.startsWith("alloc_") ? statId : "alloc_" + statId;
                    };
                    int allocCurrent = (int) getInt.invoke(result, tagKey);
                    int available = (int) getInt.invoke(result, "stat_points");

                    if (points < 0) {
                        int take = Math.min(allocCurrent, -points);
                        if (take <= 0) {
                            return;
                        }
                        putInt.invoke(result, tagKey, allocCurrent - take);
                        putInt.invoke(result, "stat_points", available + take);
                        applyModifiers(player);
                        forceSync(player, 0);
                        return;
                    }

                    // 4-way point_cost resolution: NBT int → NBT string → config expression → default 1
                    int cost = 1;
                    String costNbtKey = "point_cost_" + statId;
                    if (result instanceof net.minecraft.nbt.NbtCompound ecmTag) {
                        if (ecmTag.contains(costNbtKey, 3)) {
                            cost = ecmTag.getInt(costNbtKey);
                        } else if (ecmTag.contains(costNbtKey, 8)) {
                            String costExpr = ecmTag.getString(costNbtKey);
                            var costResult = net.puffish.skillsmod.expression.DefaultParser.parse(costExpr, java.util.Set.of("current"));
                            int cur = allocCurrent;
                            cost = costResult.getSuccess().map(expr -> {
                                try { return (int) Math.ceil(expr.eval(java.util.Map.of("current", (double) cur))); }
                                catch (Exception e2) { return 1; }
                            }).orElse(1);
                        } else {
                            // Config compiledPointCostExpression fallback
                            net.bluelotuscoding.skillleveling.bridge.config.AttributeDef slotDef = null;
                            if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp2) {
                                String cn = net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getPlatform().getEpicClassName(sp2);
                                var classDef = EpicClassConfigManager.getClassDef(cn);
                                if (classDef != null) {
                                    outer: for (var page : EpicClassConfigManager.getPagesForClass(classDef.class_name)) {
                                        if (page.slots != null) {
                                            for (var sd : page.slots) {
                                                if (statId.equals(sd.id)) { slotDef = sd; break outer; }
                                            }
                                        }
                                    }
                                }
                            }
                            if (slotDef != null && slotDef.compiledPointCostExpression != null) {
                                try {
                                    cost = (int) Math.ceil(slotDef.compiledPointCostExpression.eval(java.util.Map.of("current", (double) allocCurrent)));
                                } catch (Exception e2) {
                                    cost = 1;
                                }
                            }
                        }
                    }
                    cost = Math.max(1, cost);

                    if (available >= cost) {
                        int grant = Math.max(1, points);
                        int possible = available / cost;
                        int delta = Math.min(grant, possible);
                        if (delta <= 0) {
                            return;
                        }
                        putInt.invoke(result, tagKey, allocCurrent + delta);
                        putInt.invoke(result, "stat_points", available - (delta * cost));
                        applyModifiers(player);
                        forceSync(player, 0);
                    }
                } else {
                    AddonLogger.LOGGER.warn("[Bridge] allocateStat failed: could not resolve NBT get/put methods");
                }
            } else {
                AddonLogger.LOGGER.warn("[Bridge] allocateStat failed: PlayerLevelData.root returned null");
            }
        } catch (Exception e) {
            AddonLogger.LOGGER.error("[Bridge] allocateStat exception: " + e.getMessage());
        }
    }

    /**
     * Wipes all mod-specific data from Epic Classes for a player.
     * Removes ecm_leveling tag and resets state to default.
     */
    public static void cleanupData(PlayerEntity player) {
        try {
            // 1. Remove Forge persistent NBT
            // 1. Remove Forge persistent NBT
            // Note: PlayerEntity implements IForgeEntity on Forge/NeoForge.
            net.minecraft.nbt.NbtCompound tag = ((net.minecraftforge.common.extensions.IForgeEntity) player).getPersistentData();
            if (tag != null) {
                tag.remove("ecm_leveling");
            }

            // 2. Reset PlayerLevelData in Epic Classes via reflection
            initReflection();
            if (playerLevelDataClass != null) {
                for (java.lang.reflect.Method m : playerLevelDataClass.getMethods()) {
                    if (m.getName().equals("cleanupForModRemoval")) {
                        m.invoke(null, player);
                        break;
                    } else if (m.getName().equals("resetLevels") || m.getName().equals("resetAll")) {
                        m.invoke(null, player);
                    }
                }
            }

            // 3. Force sync to client to clear UI/client state
            forceSync(player);
        } catch (Throwable ignored) {
        }
    }
}
