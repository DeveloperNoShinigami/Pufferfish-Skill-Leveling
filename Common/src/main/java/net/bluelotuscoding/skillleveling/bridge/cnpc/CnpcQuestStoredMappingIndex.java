package net.bluelotuscoding.skillleveling.bridge.cnpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;

public final class CnpcQuestStoredMappingIndex {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, CnpcQuestMappingDef>>() {
    }.getType();
    private static final long REFRESH_INTERVAL_MS = 1000L;

    private static volatile Map<String, CnpcQuestMappingDef> cachedMappings = Map.of();
    private static volatile long lastRefreshAt;

    private CnpcQuestStoredMappingIndex() {
    }

    public static CnpcQuestMappingDef getQuestMapping(String questId) {
        if (questId == null || questId.isBlank()) {
            return null;
        }
        Map<String, CnpcQuestMappingDef> snapshot = cachedMappings;
        CnpcQuestMappingDef exact = snapshot.get(questId);
        if (exact != null) {
            return exact;
        }
        return snapshot.get(questId.trim().toLowerCase(java.util.Locale.ROOT));
    }

    public static synchronized void refresh(MinecraftServer server) {
        if (server == null) {
            cachedMappings = Map.of();
            lastRefreshAt = 0L;
            return;
        }
        long now = Util.getMeasuringTimeMs();
        if (now - lastRefreshAt < REFRESH_INTERVAL_MS) {
            return;
        }

        Map<String, CnpcQuestMappingDef> rebuilt = new LinkedHashMap<>();
        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                mergeEntityMappings(rebuilt, entity);
            }
        }
        cachedMappings = Collections.unmodifiableMap(rebuilt);
        lastRefreshAt = now;
    }

    public static synchronized void clear() {
        cachedMappings = Map.of();
        lastRefreshAt = 0L;
    }

    private static void mergeEntityMappings(Map<String, CnpcQuestMappingDef> out, Entity entity) {
        String raw = CnpcNpcRoleResolver.getStoredString(entity, CnpcNpcRoleResolver.QUEST_MAPPINGS_KEY);
        if (raw == null || raw.isBlank()) {
            return;
        }
        Map<String, CnpcQuestMappingDef> parsed;
        try {
            parsed = GSON.fromJson(raw, MAP_TYPE);
        } catch (Exception e) {
            AddonLogger.LOGGER.warn("Failed to parse CNPC stored cnpcQuestMappings for entity "
                    + entity.getName().getString() + " (" + entity.getId() + "): " + e.getMessage());
            return;
        }
        if (parsed == null || parsed.isEmpty()) {
            return;
        }
        for (Map.Entry<String, CnpcQuestMappingDef> entry : parsed.entrySet()) {
            String key = normalizeKey(entry.getKey());
            CnpcQuestMappingDef value = sanitize(entry.getValue());
            if (key == null || value == null) {
                continue;
            }
            CnpcQuestMappingDef existing = out.putIfAbsent(key, value);
            if (existing != null && !sameMapping(existing, value)) {
                AddonLogger.LOGGER.warn("Duplicate CNPC stored quest mapping for quest " + key
                        + " on entity " + entity.getName().getString() + " (" + entity.getId() + "); keeping first");
            }
        }
    }

    private static CnpcQuestMappingDef sanitize(CnpcQuestMappingDef mapping) {
        if (mapping == null) {
            return null;
        }
        if ((mapping.title == null || mapping.title.isBlank())
                && (mapping.classId == null || mapping.classId.isBlank())
                && (mapping.bookCategory == null || mapping.bookCategory.isBlank())) {
            return null;
        }
        CnpcQuestMappingDef copy = new CnpcQuestMappingDef();
        copy.title = trim(mapping.title);
        copy.classId = trim(mapping.classId);
        copy.bookCategory = trim(mapping.bookCategory);
        copy.trackStructure = trim(mapping.trackStructure);
        return copy;
    }

    private static boolean sameMapping(CnpcQuestMappingDef left, CnpcQuestMappingDef right) {
        return java.util.Objects.equals(trim(left.title), trim(right.title))
                && java.util.Objects.equals(trim(left.classId), trim(right.classId))
                && java.util.Objects.equals(trim(left.bookCategory), trim(right.bookCategory))
                && java.util.Objects.equals(trim(left.trackStructure), trim(right.trackStructure));
    }

    private static String normalizeKey(String key) {
        String trimmed = trim(key);
        return trimmed == null ? null : trimmed.toLowerCase(java.util.Locale.ROOT);
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
