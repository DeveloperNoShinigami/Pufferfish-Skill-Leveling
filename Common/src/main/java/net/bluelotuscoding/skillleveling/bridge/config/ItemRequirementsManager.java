package net.bluelotuscoding.skillleveling.bridge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.registry.Registries;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Loads item restriction definitions from datapacks under
 * data/{namespace}/epicclassmod/item_restrictions/{name}.json
 *
 * Supports both single-entry and multi-entry formats:
 *
 * Single-entry:
 * { "item": "minecraft:bow", "require_effect": ["minecraft:speed"] }
 *
 * Multi-entry:
 * { "restrictions": [ { "item": "...", ... }, { "item": [...], ... } ] }
 */
public class ItemRequirementsManager extends JsonDataLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Server-side maps
    private static final Map<String, ItemRequirementDef> ITEM_REQUIREMENTS = new HashMap<>();
    private static final Map<String, ItemRequirementDef> BLOCK_REQUIREMENTS = new HashMap<>();
    private static final Map<String, ItemRequirementDef> ENTITY_REQUIREMENTS = new HashMap<>();
    private static final Map<String, ItemRequirementDef> DIMENSION_REQUIREMENTS = new HashMap<>();
    private static final Map<String, ItemRequirementDef> STRUCTURE_REQUIREMENTS = new HashMap<>();

    // Client-side maps (populated via sync packet)
    private static final Map<String, ItemRequirementDef> CLIENT_ITEM_REQUIREMENTS = new HashMap<>();
    private static final Map<String, ItemRequirementDef> CLIENT_BLOCK_REQUIREMENTS = new HashMap<>();
    private static final Map<String, ItemRequirementDef> CLIENT_ENTITY_REQUIREMENTS = new HashMap<>();
    private static final Map<String, ItemRequirementDef> CLIENT_DIMENSION_REQUIREMENTS = new HashMap<>();
    private static final Map<String, ItemRequirementDef> CLIENT_STRUCTURE_REQUIREMENTS = new HashMap<>();

    private static Method epicClassGetAcceptedKeys = null;
    private static boolean epicClassQuestReflectionFailed = false;
    private static Method epicClassClientGetAcceptedKeys = null;
    private static boolean epicClassClientQuestReflectionFailed = false;

    public ItemRequirementsManager() {
        super(GSON, "epicclassmod/item_restrictions");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        ITEM_REQUIREMENTS.clear();
        BLOCK_REQUIREMENTS.clear();
        ENTITY_REQUIREMENTS.clear();
        DIMENSION_REQUIREMENTS.clear();
        STRUCTURE_REQUIREMENTS.clear();

        AddonLogger.LOGGER.info("[ADDON] Reloading restrictions. Found " + prepared.size() + " files.");

        for (Map.Entry<Identifier, JsonElement> entry : prepared.entrySet()) {
            try {
                JsonObject root = entry.getValue().getAsJsonObject();
                AddonLogger.LOGGER.info("[ADDON] Parsing restriction file: " + entry.getKey());

                // Multi-entry format: { "restrictions": [ ... ] }
                if (root.has("restrictions")) {
                    JsonArray arr = root.getAsJsonArray("restrictions");
                    for (JsonElement elem : arr) {
                        parseAndRegister(elem.getAsJsonObject(), entry.getKey());
                    }
                } else {
                    // Single-entry format: { "item": "...", ... }
                    parseAndRegister(root, entry.getKey());
                }
            } catch (Exception e) {
                AddonLogger.LOGGER.error(
                        "Failed to load item requirement " + entry.getKey() + ": " + e.getMessage());
            }
        }

        AddonLogger.LOGGER.info("[ADDON] Final restriction count -> " + ITEM_REQUIREMENTS.size() + " Item, " +
                BLOCK_REQUIREMENTS.size() + " Block, " +
                ENTITY_REQUIREMENTS.size() + " Entity, " +
                DIMENSION_REQUIREMENTS.size() + " Dimension, " +
                STRUCTURE_REQUIREMENTS.size() + " Structure.");
    }

    private void parseAndRegister(JsonObject obj, Identifier sourceId) {
        ItemRequirementDef def = GSON.fromJson(obj, ItemRequirementDef.class);

        // Standardize singular/plural mapping for all types
        def.items = parseSingularPlural(obj, "item", "items");
        for (String id : def.items)
            ITEM_REQUIREMENTS.put(id, def);

        def.blocks = parseSingularPlural(obj, "block", "blocks");
        for (String id : def.blocks)
            BLOCK_REQUIREMENTS.put(id, def);

        def.entities = parseSingularPlural(obj, "entity", "entities");
        for (String id : def.entities)
            ENTITY_REQUIREMENTS.put(id, def);

        def.dimensions = parseSingularPlural(obj, "dimension", "dimensions");
        for (String id : def.dimensions)
            DIMENSION_REQUIREMENTS.put(id, def);

        def.structures = parseSingularPlural(obj, "structure", "structures");
        def.structures.addAll(parseSingularPlural(obj, "dungeon", "dungeons"));
        for (String id : def.structures)
            STRUCTURE_REQUIREMENTS.put(id, def);
    }

    private List<String> parseSingularPlural(JsonObject obj, String sing, String plur) {
        List<String> list = parseListField(obj, plur);
        if (list.isEmpty()) {
            list = parseListField(obj, sing);
        }
        return list;
    }

    private List<String> parseListField(JsonObject obj, String fieldName) {
        List<String> list = new ArrayList<>();
        if (obj.has(fieldName)) {
            JsonElement elem = obj.get(fieldName);
            if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
                list.add(elem.getAsString());
            } else if (elem.isJsonArray()) {
                JsonArray arr = elem.getAsJsonArray();
                for (JsonElement e : arr) {
                    if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                        list.add(e.getAsString());
                    }
                }
            }
        }
        return list;
    }

    // ---- Accessors for Syncing ----

    public static Map<String, ItemRequirementDef> getItemRequirements() {
        return Collections.unmodifiableMap(ITEM_REQUIREMENTS);
    }

    public static Map<String, ItemRequirementDef> getServerRequirementsMap() {
        Map<String, ItemRequirementDef> all = new HashMap<>();
        all.putAll(ITEM_REQUIREMENTS);
        all.putAll(BLOCK_REQUIREMENTS);
        all.putAll(ENTITY_REQUIREMENTS);
        all.putAll(DIMENSION_REQUIREMENTS);
        all.putAll(STRUCTURE_REQUIREMENTS);
        return all;
    }

    public static Map<String, ItemRequirementDef> getBlockRequirements() {
        return Collections.unmodifiableMap(BLOCK_REQUIREMENTS);
    }

    public static Map<String, ItemRequirementDef> getEntityRequirements() {
        return Collections.unmodifiableMap(ENTITY_REQUIREMENTS);
    }

    public static Map<String, ItemRequirementDef> getDimensionRequirements() {
        return Collections.unmodifiableMap(DIMENSION_REQUIREMENTS);
    }

    public static Map<String, ItemRequirementDef> getStructureRequirements() {
        return Collections.unmodifiableMap(STRUCTURE_REQUIREMENTS);
    }

    public static void setClientRequirements(
            Map<String, ItemRequirementDef> items,
            Map<String, ItemRequirementDef> blocks,
            Map<String, ItemRequirementDef> entities,
            Map<String, ItemRequirementDef> dimensions,
            Map<String, ItemRequirementDef> structures) {

        CLIENT_ITEM_REQUIREMENTS.clear();
        CLIENT_ITEM_REQUIREMENTS.putAll(items);

        CLIENT_BLOCK_REQUIREMENTS.clear();
        CLIENT_BLOCK_REQUIREMENTS.putAll(blocks);

        CLIENT_ENTITY_REQUIREMENTS.clear();
        CLIENT_ENTITY_REQUIREMENTS.putAll(entities);

        CLIENT_DIMENSION_REQUIREMENTS.clear();
        CLIENT_DIMENSION_REQUIREMENTS.putAll(dimensions);

        CLIENT_STRUCTURE_REQUIREMENTS.clear();
        CLIENT_STRUCTURE_REQUIREMENTS.putAll(structures);

        AddonLogger.LOGGER.info("Client synced restrictions: " + items.size() + " Item, " + blocks.size() + " Block, " +
                entities.size() + " Entity, " + dimensions.size() + " Dim, " + structures.size() + " Struct.");
    }

    public static ItemRequirementDef getRequirements(String id, TargetType type) {
        if (SkillLevelingMod.getInstance().getPlatform().isClient()) {
            return switch (type) {
                case ITEM -> CLIENT_ITEM_REQUIREMENTS.get(id);
                case BLOCK -> CLIENT_BLOCK_REQUIREMENTS.get(id);
                case ENTITY -> CLIENT_ENTITY_REQUIREMENTS.get(id);
                case DIMENSION -> CLIENT_DIMENSION_REQUIREMENTS.get(id);
                case STRUCTURE -> CLIENT_STRUCTURE_REQUIREMENTS.get(id);
            };
        }
        return switch (type) {
            case ITEM -> ITEM_REQUIREMENTS.get(id);
            case BLOCK -> BLOCK_REQUIREMENTS.get(id);
            case ENTITY -> ENTITY_REQUIREMENTS.get(id);
            case DIMENSION -> DIMENSION_REQUIREMENTS.get(id);
            case STRUCTURE -> STRUCTURE_REQUIREMENTS.get(id);
        };
    }

    public static ItemRequirementDef getRequirements(String itemId) {
        return getRequirements(itemId, TargetType.ITEM);
    }

    public enum TargetType {
        ITEM, BLOCK, ENTITY, DIMENSION, STRUCTURE
    }

    // ---- Requirement Checking ----

    public static List<String> checkRequirements(PlayerEntity player, String id, TargetType type) {
        ItemRequirementDef req = getRequirements(id, type);
        if (req == null) {
            return Collections.emptyList();
        }

        List<String> failures = new ArrayList<>();

        // Environmental Gating (Always check if present in any def)
        if (req.in_water != null && player.isTouchingWater() != req.in_water) {
            failures.add(req.in_water ? "Must be in water" : "Must be on land");
        }

        if (req.time_of_day != null) {
            long time = player.getWorld().getTimeOfDay() % 24000L;
            if (time < req.time_of_day.min || time > req.time_of_day.max) {
                failures.add("Restricted by time of day (" + req.time_of_day.min + "-" + req.time_of_day.max + ")");
            }
        }

        if (req.require_class != null) {
            String pClass = "";
            try {
                // On client, try ClientCustomClassState first
                if (player.getWorld().isClient()) {
                    pClass = net.bluelotuscoding.skillleveling.client.ClientCustomClassState
                            .getCustomClass(player.getUuid());
                } else {
                    pClass = SkillLevelingMod.getInstance().getPlatform().getEpicClassName(player);
                }
            } catch (Exception e) {
                pClass = "";
            }

            // Match flexibly: "Lich" matches "epic_classes:lich", "lich", "Lich"
            if (!classNameMatches(req.require_class, pClass)) {
                failures.add("Requires Class: " + req.require_class);
            }
        }

        if (req.require_level != null) {
            try {
                Identifier catId = new Identifier(req.require_level.category);
                int pLvl = SkillLevelingMod.getInstance().getPlatform().getPufferfishLevel(player, catId);
                if (pLvl < req.require_level.min) {
                    failures.add("Requires Level: " + req.require_level.min
                            + " in " + req.require_level.category);
                }
            } catch (Exception e) {
                // Server-side API not available on client, show as unmet
                failures.add("Requires Level: " + req.require_level.min
                        + " in " + req.require_level.category);
            }
        }

        if (req.require_attribute != null) {
            try {
                net.minecraft.entity.attribute.EntityAttribute attr = Registries.ATTRIBUTE
                        .get(new Identifier(req.require_attribute.attribute));
                if (attr != null) {
                    double pVal = player.getAttributeValue(attr);
                    if (pVal < req.require_attribute.min) {
                        failures.add("Requires Attribute: " + req.require_attribute.min
                                + " " + req.require_attribute.attribute);
                    }
                }
            } catch (Exception e) {
                // Attribute not found, skip
            }
        }

        if (req.require_held != null && !req.require_held.isEmpty()) {
            boolean matches = false;
            String mainHand = Registries.ITEM.getId(player.getMainHandStack().getItem()).toString();
            String offHand = Registries.ITEM.getId(player.getOffHandStack().getItem()).toString();
            for (String h : req.require_held) {
                if (h.equals(mainHand) || h.equals(offHand)) {
                    matches = true;
                    break;
                }
            }
            if (!matches) {
                failures.add("Must Hold: " + String.join(", ", req.require_held));
            }
        }

        if (req.require_worn != null && !req.require_worn.isEmpty()) {
            boolean matches = false;
            for (net.minecraft.item.ItemStack armor : player.getArmorItems()) {
                String armorId = Registries.ITEM.getId(armor.getItem()).toString();
                if (req.require_worn.contains(armorId)) {
                    matches = true;
                    break;
                }
            }
            if (!matches) {
                failures.add("Must Wear: " + String.join(", ", req.require_worn));
            }
        }

        if (req.require_effect != null && !req.require_effect.isEmpty()) {
            for (String effectId : req.require_effect) {
                net.minecraft.entity.effect.StatusEffect effect = Registries.STATUS_EFFECT
                        .get(new Identifier(effectId));
                if (effect == null || !player.hasStatusEffect(effect)) {
                    failures.add("Requires Effect: " + effectId);
                }
            }
        }

        if (req.require_quest != null && !req.require_quest.isEmpty()) {
            Set<String> acceptedQuests = getEpicClassQuests(player);
            for (String qId : req.require_quest) {
                if (acceptedQuests == null || !acceptedQuests.contains(qId)) {
                    failures.add("Requires Quest: " + qId);
                }
            }
        }

        return failures;
    }

    /**
     * Flexible class name matching.
     * Handles namespaced IDs (e.g. "epic_classes:lich") and plain names ("Lich").
     * Both "Lich" and "lich" will match "epic_classes:lich" or "epic_classes:Lich".
     */
    private static boolean classNameMatches(String required, String actual) {
        if (required == null || actual == null || actual.isEmpty()) {
            return false;
        }
        // Direct case-insensitive match
        if (required.equalsIgnoreCase(actual)) {
            return true;
        }
        // Strip namespace from actual (e.g. "epic_classes:lich" -> "lich")
        String actualName = actual;
        int colonIdx = actual.indexOf(':');
        if (colonIdx >= 0) {
            actualName = actual.substring(colonIdx + 1);
        }
        // Strip namespace from required too (in case they put "epic_classes:lich" in
        // JSON)
        String requiredName = required;
        int reqColonIdx = required.indexOf(':');
        if (reqColonIdx >= 0) {
            requiredName = required.substring(reqColonIdx + 1);
        }
        // Handle "none" case
        if (actualName.equalsIgnoreCase("none")) {
            return false;
        }
        return requiredName.equalsIgnoreCase(actualName);
    }

    // ---- Quest Reflection ----

    @SuppressWarnings("unchecked")
    public static Set<String> getEpicClassQuests(PlayerEntity player) {
        if (player.getWorld().isClient()) {
            if (epicClassClientQuestReflectionFailed) {
                return null;
            }
            try {
                if (epicClassClientGetAcceptedKeys == null) {
                    // Try to find the client quest state class in the actual mod package
                    Class<?> pq = null;
                    try {
                        pq = Class.forName("net.bluelotuscoding.skillleveling.bridge.forge.ClientQuestState");
                    } catch (Exception e) {
                        try {
                            pq = Class.forName("com.example.epicclassmod.client.ClientQuestState");
                        } catch (Exception e2) {
                            throw new ClassNotFoundException("Quest state class not found");
                        }
                    }
                    epicClassClientGetAcceptedKeys = pq.getMethod("getAcceptedKeys");
                }
                return (Set<String>) epicClassClientGetAcceptedKeys.invoke(null);
            } catch (Exception e) {
                epicClassClientQuestReflectionFailed = true;
                AddonLogger.LOGGER.error("Failed to reflect Epic Class ClientQuestState: " + e.getMessage());
                return null;
            }
        } else {
            if (epicClassQuestReflectionFailed) {
                return null;
            }
            try {
                if (epicClassGetAcceptedKeys == null) {
                    Class<?> pq = null;
                    try {
                        pq = Class.forName("net.bluelotuscoding.skillleveling.bridge.forge.PlayerQuests");
                    } catch (Exception e) {
                        try {
                            pq = Class.forName("com.example.epicclassmod.data.quest.PlayerQuests");
                        } catch (Exception e2) {
                            throw new ClassNotFoundException("Player quests class not found");
                        }
                    }
                    epicClassGetAcceptedKeys = pq.getMethod("getAcceptedKeys",
                            net.minecraft.server.network.ServerPlayerEntity.class);
                }
                return (Set<String>) epicClassGetAcceptedKeys.invoke(null, player);
            } catch (Exception e) {
                epicClassQuestReflectionFailed = true;
                AddonLogger.LOGGER.error("Failed to reflect Epic Class PlayerQuests: " + e.getMessage());
                return null;
            }
        }
    }
}
