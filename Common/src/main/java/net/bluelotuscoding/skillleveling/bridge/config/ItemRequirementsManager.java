package net.bluelotuscoding.skillleveling.bridge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.HashSet;

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
        List<String> failures = new ArrayList<>();

        if (req == null) {
            if (type == TargetType.ITEM && isAutoClassWeaponRestrictionsEnabled()) {
                addClassWeaponFailures(player, id, failures);
            }
            return failures;
        }

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

        // Avoid conflicts with the legacy explicit class requirement path.
        // If require_class is set, it remains the sole class-gating source.
        if (type == TargetType.ITEM
                && isAutoClassWeaponRestrictionsEnabled()
                && (req.require_class == null || req.require_class.isBlank())) {
            addClassWeaponFailures(player, id, failures);
        }

        return failures;
    }

    private static boolean isAutoClassWeaponRestrictionsEnabled() {
        try {
            var cfg = net.bluelotuscoding.skillleveling.bridge.BridgeConfigManager.getConfig();
            if (cfg != null) {
                return cfg.enableAutoClassWeaponRestrictions;
            }
        } catch (Exception ignored) {
        }

        try {
            var synced = EpicClassConfigManager.getSyncedConfig();
            if (synced != null) {
                return synced.enableAutoClassWeaponRestrictions;
            }
        } catch (Exception ignored) {
        }

        return true;
    }

    private static void addClassWeaponFailures(PlayerEntity player, String itemId, List<String> failures) {
        try {
            if (player == null || itemId == null || itemId.isBlank()) {
                return;
            }

            Identifier itemIdentifier = new Identifier(itemId);
            Item item = Registries.ITEM.get(itemIdentifier);
            if (item == null || item == Items.AIR) {
                return;
            }

            String currentClass = resolveCurrentClassId(player);

            if (currentClass == null || currentClass.isBlank() || "epic_classes:none".equalsIgnoreCase(currentClass)
                    || "none".equalsIgnoreCase(currentClass)) {
                return;
            }

            EpicClassDef classDef = EpicClassConfigManager.getClassDef(currentClass);
            if (classDef == null) {
                return;
            }

            Set<String> allowedItems = new HashSet<>();
            List<TagKey<Item>> allowedTags = new ArrayList<>();
            collectClassWeaponAllowRules(classDef, allowedItems, allowedTags);

            // If class tree defines no weapon allow-list, do not auto-restrict.
            if (allowedItems.isEmpty() && allowedTags.isEmpty()) {
                return;
            }

            // Only apply automatic class restrictions to weapon-like items.
            // A configured class weapon tag is treated as weapon-like, allowing any
            // namespace tag (not just forge/c) to define the weapon domain.
            if (!isWeaponLike(item, allowedTags)) {
                return;
            }

            if (allowedItems.contains(itemId.toLowerCase(java.util.Locale.ROOT))) {
                return;
            }

            var itemEntry = Registries.ITEM.getEntry(item);
            for (TagKey<Item> tag : allowedTags) {
                if (itemEntry.isIn(tag)) {
                    return;
                }
            }

            failures.add(buildClassRequirementMessage(itemId, item));
        } catch (Exception ignored) {
            // Fail-open to avoid false positives from malformed datapack values.
        }
    }

    private static String resolveCurrentClassId(PlayerEntity player) {
        if (player == null) {
            return "";
        }

        String currentClass = "";
        if (player.getWorld().isClient()) {
            currentClass = net.bluelotuscoding.skillleveling.client.ClientCustomClassState
                    .getCustomClass(player.getUuid());
        }

        if (currentClass == null || currentClass.isBlank()
                || "epic_classes:none".equalsIgnoreCase(currentClass)
                || "none".equalsIgnoreCase(currentClass)) {
            try {
                currentClass = SkillLevelingMod.getInstance().getPlatform().getEpicClassName(player);
            } catch (Exception ignored) {
                currentClass = "";
            }
        }

        return currentClass == null ? "" : currentClass;
    }

    private static String buildClassRequirementMessage(String itemId, Item item) {
        String normalizedItemId = itemId.toLowerCase(java.util.Locale.ROOT);
        Set<String> seenClasses = new HashSet<>();
        List<String> matchingClassNames = new ArrayList<>();
        var itemEntry = Registries.ITEM.getEntry(item);

        for (EpicClassDef def : EpicClassConfigManager.getClasses().values()) {
            if (def == null || def.class_name == null || def.class_name.isBlank()) {
                continue;
            }

            String classKey = EpicClassBridge.normalizeClassName(def.class_name);
            if (!seenClasses.add(classKey)) {
                continue;
            }

            if (!classWeaponRuleDefinedOnClass(def)) {
                continue;
            }

            if (!classWeaponRuleMatchesDirectly(def, normalizedItemId, itemEntry)) {
                continue;
            }

            String display = def.display_name;
            if (display == null || display.isBlank()) {
                display = def.gui_title;
            }
            if (display == null || display.isBlank()) {
                display = humanizeClassName(classKey);
            }
            matchingClassNames.add(display);
        }

        if (matchingClassNames.isEmpty()) {
            return "Requires Class Weapon";
        }

        if (matchingClassNames.size() == 1) {
            return "Requires Class: " + matchingClassNames.get(0);
        }

        return "Requires Class: " + String.join(", ", matchingClassNames);
    }

    private static String humanizeClassName(String normalizedClassName) {
        if (normalizedClassName == null || normalizedClassName.isBlank()) {
            return "Unknown";
        }

        String[] parts = normalizedClassName.replace('-', ' ').replace('_', ' ').trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.toString();
    }

    private static void collectClassWeaponAllowRules(EpicClassDef root, Set<String> allowedItems,
            List<TagKey<Item>> allowedTags) {
        Set<String> visited = new HashSet<>();
        EpicClassDef current = root;
        while (current != null) {
            String key = current.class_name != null && !current.class_name.isBlank()
                    ? EpicClassBridge.normalizeClassName(current.class_name)
                    : Integer.toHexString(System.identityHashCode(current));
            if (!visited.add(key)) {
                break;
            }

            if (current.class_weapon_items != null) {
                for (String value : current.class_weapon_items) {
                    if (value != null && !value.isBlank()) {
                        allowedItems.add(value.trim().toLowerCase(java.util.Locale.ROOT));
                    }
                }
            }

            if (current.class_weapon_tags != null) {
                for (String rawTag : current.class_weapon_tags) {
                    TagKey<Item> tagKey = parseItemTag(rawTag);
                    if (tagKey != null && !allowedTags.contains(tagKey)) {
                        allowedTags.add(tagKey);
                    }
                }
            }

            if (current.class_parent == null || current.class_parent.isBlank()) {
                break;
            }
            current = EpicClassConfigManager.getClassDef(current.class_parent);
        }
    }

    private static boolean classWeaponRuleDefinedOnClass(EpicClassDef def) {
        return def != null
                && ((def.class_weapon_items != null && !def.class_weapon_items.isEmpty())
                        || (def.class_weapon_tags != null && !def.class_weapon_tags.isEmpty()));
    }

    private static boolean classWeaponRuleMatchesDirectly(EpicClassDef def, String normalizedItemId,
            net.minecraft.registry.entry.RegistryEntry<Item> itemEntry) {
        if (def == null) {
            return false;
        }

        if (def.class_weapon_items != null) {
            for (String value : def.class_weapon_items) {
                if (value != null && !value.isBlank()
                        && normalizedItemId.equals(value.trim().toLowerCase(java.util.Locale.ROOT))) {
                    return true;
                }
            }
        }

        if (def.class_weapon_tags != null) {
            for (String rawTag : def.class_weapon_tags) {
                TagKey<Item> tagKey = parseItemTag(rawTag);
                if (tagKey != null && itemEntry.isIn(tagKey)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static TagKey<Item> parseItemTag(String rawTag) {
        if (rawTag == null || rawTag.isBlank()) {
            return null;
        }

        String normalized = rawTag.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        try {
            return TagKey.of(RegistryKeys.ITEM, new Identifier(normalized));
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isWeaponLike(Item item, List<TagKey<Item>> classWeaponTags) {
        if (item instanceof SwordItem || item instanceof AxeItem || item instanceof BowItem
                || item instanceof CrossbowItem || item instanceof TridentItem || item instanceof ShieldItem) {
            return true;
        }

        var itemEntry = Registries.ITEM.getEntry(item);
        if (classWeaponTags != null) {
            for (TagKey<Item> tag : classWeaponTags) {
                if (itemEntry.isIn(tag)) {
                    return true;
                }
            }
        }

        return false;
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

        // Handle "none" case
        if (actual.equalsIgnoreCase("none") || actual.equalsIgnoreCase("epic_classes:none")) {
            return false;
        }

        // Use the bridge's inheritance-aware check
        return EpicClassBridge.isClassOrParent(actual, required);
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
