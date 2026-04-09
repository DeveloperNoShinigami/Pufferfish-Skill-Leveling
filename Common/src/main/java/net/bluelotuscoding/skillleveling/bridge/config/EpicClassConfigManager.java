package net.bluelotuscoding.skillleveling.bridge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import net.minecraft.util.Identifier;

public class EpicClassConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static File epicClassesDir;
    private static File attributesFile;
    private static File classesDir;
    private static java.util.Map<String, java.util.List<ClassPageDef>> classAttributePages = new java.util.HashMap<>();
    private static java.util.Map<String, EpicClassDef> classDefinitions = new java.util.HashMap<>();
    private static net.bluelotuscoding.skillleveling.bridge.BridgeConfig syncedConfig = new net.bluelotuscoding.skillleveling.bridge.BridgeConfig();

    public static void setSyncedConfig(net.bluelotuscoding.skillleveling.bridge.BridgeConfig config) {
        if (config != null) {
            syncedConfig = config;
        }
    }

    public static net.bluelotuscoding.skillleveling.bridge.BridgeConfig getSyncedConfig() {
        return syncedConfig;
    }

    public static void load(File configDir) {
        // No-op for now as logic moves to DataLoaders
    }

    public static void setClasses(Map<Identifier, EpicClassDef> classes) {
        classDefinitions.clear();
        classes.forEach((id, def) -> {
            String name = def.class_name != null ? def.class_name : id.toString();
            classDefinitions.put(name.toLowerCase(java.util.Locale.ROOT), def);
        });
    }

    public static void setAttributePages(Map<String, List<ClassPageDef>> pages) {
        classAttributePages.clear();
        classAttributePages.putAll(pages);
    }

    public static Map<String, List<ClassPageDef>> getAttributePagesMap() {
        return classAttributePages;
    }

    public static void setClassesOnClient(Map<String, EpicClassDef> classes) {
        classDefinitions.clear();
        classDefinitions.putAll(classes);
    }

    public static void setAttributePagesOnClient(Map<String, List<ClassPageDef>> pages) {
        classAttributePages.clear();
        pages.forEach((key, list) -> {
            for (ClassPageDef page : list) {
                if (page.slots != null) {
                    for (AttributeDef def : page.slots) {
                        if (def != null && def.value != null) {
                            var result = net.puffish.skillsmod.expression.DefaultParser.parse(def.value,
                                    java.util.Set.of("points"));
                            def.compiledExpression = result.getSuccess().orElse(null);
                        }
                        if (def != null && def.point_cost != null && !def.point_cost.isBlank()) {
                            var costResult = net.puffish.skillsmod.expression.DefaultParser.parse(def.point_cost,
                                    java.util.Set.of("current"));
                            def.compiledPointCostExpression = costResult.getSuccess().orElse(null);
                        }
                    }
                }
            }
        });
        classAttributePages.putAll(pages);
    }

    private static void generateDefaultClass() {
        File defaultClassFile = new File(classesDir, "necromancer.json");
        try (Writer writer = Files.newBufferedWriter(defaultClassFile.toPath(), StandardCharsets.UTF_8)) {
            JsonObject root = new JsonObject();
            root.addProperty("class_name", "epic_classes:necromancer");
            root.addProperty("gui_title", "Necromancer");
            root.addProperty("gui_description", "A master of the dark arts who summons minions.");

            JsonArray notes = new JsonArray();
            notes.add("A master of the dark arts.");
            notes.add("Summons minions to do their bidding.");
            root.add("gui_notes", notes);

            JsonArray stats = new JsonArray();
            JsonObject health = new JsonObject();
            health.addProperty("label_key", "Health");
            health.addProperty("count", 15);
            stats.add(health);
            root.add("gui_stats", stats);

            JsonArray passives = new JsonArray();
            JsonObject passive = new JsonObject();
            passive.addProperty("name_key", "Life Leech");
            passive.addProperty("desc_key", "Heals for 10% of spell damage.");
            passives.add(passive);
            root.add("gui_passives", passives);

            root.addProperty("preview_armor_base", "netherite");
            root.addProperty("preview_mainhand_item", "minecraft:bone");
            root.addProperty("preview_offhand_item", "minecraft:wither_skeleton_skull");

            JsonArray startingItems = new JsonArray();
            startingItems.add("minecraft:bone_meal");
            root.add("starting_items", startingItems);

            JsonObject attributes = new JsonObject();
            JsonObject mana = new JsonObject();
            mana.addProperty("value", 20.0);
            mana.addProperty("operation", "ADDITION");
            attributes.add("epicclassmod:max_mana", mana);
            root.add("attributes", attributes);

            GSON.toJson(root, writer);
            net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER
                    .info("Generated default class: " + defaultClassFile.getName());
        } catch (Exception e) {
            net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER.warn("Failed to generate default class config.");
        }
    }

    private static void generateDefaultAttributes() {
        try (Writer writer = Files.newBufferedWriter(attributesFile.toPath(), StandardCharsets.UTF_8)) {
            JsonObject root = new JsonObject();
            JsonObject attributesByClass = new JsonObject();

            JsonArray necromancerPages = new JsonArray();
            JsonObject page0 = new JsonObject();
            page0.addProperty("id", "page_0");
            JsonArray slots = new JsonArray();

            JsonObject mana = new JsonObject();
            mana.addProperty("id", "max_mana");
            mana.addProperty("attribute_id", "epicfight:max_mana");
            mana.addProperty("icon", "irons_spellbooks:mana_upgrade_orb");
            mana.addProperty("format", "+#.# Max Mana");
            mana.addProperty("value", "points * 5.0");
            mana.addProperty("operation", "ADDITION");
            mana.addProperty("max_points", 20);
            mana.addProperty("description", "Increases your maximum mana capacity.");
            slots.add(mana);

            page0.add("slots", slots);
            necromancerPages.add(page0);

            // Add a second page for pagination testing
            JsonObject page1 = new JsonObject();
            page1.addProperty("id", "page_1");
            JsonArray slots1 = new JsonArray();
            JsonObject regen = new JsonObject();
            regen.addProperty("id", "mana_regen");
            regen.addProperty("attribute_id", "epicfight:mana_regen");
            regen.addProperty("icon", "minecraft:ghast_tear");
            regen.addProperty("format", "+#.# Mana Regen");
            regen.addProperty("value", "points * 0.1");
            regen.addProperty("operation", "ADDITION");
            regen.addProperty("max_points", 10);
            regen.addProperty("description", "Increases your mana regeneration speed.");
            slots1.add(regen);
            page1.add("slots", slots1);
            necromancerPages.add(page1);

            attributesByClass.add("necromancer", necromancerPages);

            root.add("attributes_by_class", attributesByClass);
            GSON.toJson(root, writer);
        } catch (Exception e) {
            net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER
                    .warn("Failed to generate default attributes file.");
        }
    }

    public static List<ClassPageDef> getPagesForClass(String className) {
        if (className == null)
            return java.util.Collections.emptyList();
        String normalized = className.toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("epic_classes:")) {
            normalized = normalized.substring("epic_classes:".length());
        }
        List<ClassPageDef> result = new java.util.ArrayList<>();
        result.addAll(classAttributePages.getOrDefault("global", java.util.Collections.emptyList()));
        result.addAll(classAttributePages.getOrDefault(normalized, java.util.Collections.emptyList()));
        return result;
    }

    public static Map<String, EpicClassDef> getClasses() {
        return classDefinitions;
    }

    public static EpicClassDef getClassDef(String className) {
        if (className == null || className.isEmpty())
            return null;
        String normalized = className.toLowerCase(java.util.Locale.ROOT);
        EpicClassDef def = classDefinitions.get(normalized);
        if (def != null)
            return def;

        // 2. Try with epic_classes prefix if missing
        if (!normalized.contains(":")) {
            def = classDefinitions.get("epic_classes:" + normalized);
            if (def != null)
                return def;
        }

        // 3. Try without prefix if present
        if (normalized.contains(":")) {
            String stripped = normalized.split(":")[1];
            def = classDefinitions.get(stripped);
            if (def != null)
                return def;
        }

        // 4. Fuzzy search: find any key that ends with the requested path
        String searchPath = normalized.contains(":") ? normalized.split(":")[1] : normalized;
        for (Map.Entry<String, EpicClassDef> entry : classDefinitions.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith(":" + searchPath)) {
                return entry.getValue();
            }
        }

        return null;
    }

    public static List<EpicClassDef> getChildClasses(String parentClassId) {
        if (parentClassId == null)
            return java.util.Collections.emptyList();
        String normalizedParent = parentClassId.toLowerCase(java.util.Locale.ROOT);
        if (normalizedParent.startsWith("epic_classes:")) {
            normalizedParent = normalizedParent.substring("epic_classes:".length());
        }

        List<EpicClassDef> children = new java.util.ArrayList<>();
        for (EpicClassDef def : classDefinitions.values()) {
            if (def.class_parent != null) {
                String normalizedDefParent = def.class_parent.toLowerCase(java.util.Locale.ROOT);
                if (normalizedDefParent.startsWith("epic_classes:")) {
                    normalizedDefParent = normalizedDefParent.substring("epic_classes:".length());
                }
                if (normalizedDefParent.equals(normalizedParent)) {
                    children.add(def);
                }
            }
        }
        return children;
    }

}
