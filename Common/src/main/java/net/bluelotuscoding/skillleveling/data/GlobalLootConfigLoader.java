package net.bluelotuscoding.skillleveling.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.List;
import java.util.Map;

public class GlobalLootConfigLoader extends JsonDataLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final GlobalLootConfig config = new GlobalLootConfig();

    public GlobalLootConfigLoader() {
        super(GSON, "global_loot_configuration");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        config.entityDrops.clear();
        config.chestInjections.clear();

        for (var entry : prepared.entrySet()) {
            try {
                JsonElement element = entry.getValue();
                if (element.isJsonObject()) {
                    JsonObject json = element.getAsJsonObject();
                    parseFile(json);
                }
            } catch (Exception e) {
                SkillLevelingMod.getInstance().getLogger()
                        .error("Error parsing loot config " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    private void parseFile(JsonObject json) {
        if (json.has("entity_loot")) {
            JsonArray entityLoot = json.getAsJsonArray("entity_loot");
            for (JsonElement e : entityLoot) {
                JsonObject obj = e.getAsJsonObject();
                GlobalLootConfig.EntityDropGroup group = new GlobalLootConfig.EntityDropGroup();

                if (obj.has("entity_types")) {
                    for (JsonElement t : obj.getAsJsonArray("entity_types")) {
                        group.entityTypes.add(t.getAsString());
                    }
                }

                if (obj.has("entries")) {
                    parseEntries(obj.getAsJsonArray("entries"), group.entries);
                }

                config.entityDrops.add(group);
            }
        }

        if (json.has("chest_loot")) {
            JsonArray chestLoot = json.getAsJsonArray("chest_loot");
            for (JsonElement e : chestLoot) {
                JsonObject obj = e.getAsJsonObject();
                GlobalLootConfig.ChestInjectionGroup group = new GlobalLootConfig.ChestInjectionGroup();

                if (obj.has("containers")) {
                    for (JsonElement c : obj.getAsJsonArray("containers")) {
                        group.containers.add(c.getAsString());
                    }
                }

                if (obj.has("entries")) {
                    parseEntries(obj.getAsJsonArray("entries"), group.entries);
                }

                config.chestInjections.add(group);
            }
        }
    }

    private void parseEntries(JsonArray array, List<GlobalLootConfig.LootEntry> target) {
        for (JsonElement e : array) {
            JsonObject obj = e.getAsJsonObject();
            GlobalLootConfig.LootEntry entry = new GlobalLootConfig.LootEntry();

            if (obj.has("type"))
                entry.type = obj.get("type").getAsString();
            if (obj.has("item"))
                entry.item = obj.get("item").getAsString();
            if (obj.has("chance"))
                entry.chance = obj.get("chance").getAsFloat();
            if (obj.has("weight"))
                entry.weight = obj.get("weight").getAsInt();
            if (obj.has("min_level"))
                entry.minLevel = obj.get("min_level").getAsInt();
            if (obj.has("max_level"))
                entry.maxLevel = obj.get("max_level").getAsInt();
            if (obj.has("nbt"))
                entry.nbt = obj.get("nbt").getAsString();

            target.add(entry);
        }
    }

    public GlobalLootConfig getConfig() {
        return config;
    }
}
