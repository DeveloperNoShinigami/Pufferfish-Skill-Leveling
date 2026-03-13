package net.bluelotuscoding.skillleveling.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.EnchantmentCostConfig;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads Exp Tome definitions from datapacks.
 * Path: data/[namespace]/tome_config/*.json
 */
public class ExpTomeConfigLoader extends JsonDataLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, ExpTomeDefinition> TOMES = new HashMap<>();

    public ExpTomeConfigLoader() {
        super(GSON, "tome_config");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        TOMES.clear();
        prepared.forEach((id, element) -> {
            try {
                if (element.isJsonObject()) {
                    JsonObject json = element.getAsJsonObject();
                    if (json.has("tomes") && json.get("tomes").isJsonArray()) {
                        json.getAsJsonArray("tomes").forEach(tomeElement -> {
                            if (tomeElement.isJsonObject()) {
                                parseAndAddTome(tomeElement.getAsJsonObject());
                            }
                        });
                    } else {
                        // Support single tome per file as well
                        parseAndAddTome(json);
                    }
                }
            } catch (Exception e) {
                SkillLevelingMod.getInstance().getLogger()
                        .error("Error loading Exp Tome config " + id + ": " + e.getMessage());
            }
        });
        SkillLevelingMod.getInstance().getLogger().info("Loaded " + TOMES.size() + " Exp Tome definitions.");
    }

    private void parseAndAddTome(JsonObject json) {
        if (!json.has("id"))
            return;

        String id = json.get("id").getAsString();
        String name = json.has("name") ? json.get("name").getAsString() : "Experience Tome";
        String rarity = json.has("rarity") ? json.get("rarity").getAsString() : "COMMON";
        int maxLevels = json.has("max_levels") ? json.get("max_levels").getAsInt() : 1;

        EnchantmentCostConfig experiencePerLevel = null;
        if (json.has("experience_per_level")) {
            JsonObject exprObj = json.getAsJsonObject("experience_per_level");
            String type = exprObj.get("type").getAsString();
            JsonObject data = exprObj.getAsJsonObject("data");

            if ("values".equals(type)) {
                com.google.gson.JsonArray arr = data.getAsJsonArray("values");
                int[] values = new int[arr.size()];
                for (int i = 0; i < arr.size(); i++)
                    values[i] = arr.get(i).getAsInt();
                experiencePerLevel = new EnchantmentCostConfig(values);
            } else if ("expression".equals(type)) {
                String expr = data.get("expression").getAsString();
                experiencePerLevel = new EnchantmentCostConfig(expr);
            }
        }

        if (experiencePerLevel != null) {
            TOMES.put(id, new ExpTomeDefinition(name, rarity, maxLevels, experiencePerLevel));
        }
    }

    public static void setAllOnClient(Map<String, ExpTomeDefinition> definitions) {
        TOMES.clear();
        if (definitions != null) {
            definitions.forEach((id, def) -> {
                if (def.experiencePerLevel != null) {
                    def.experiencePerLevel.recompile();
                }
                TOMES.put(id, def);
            });
        }
    }

    public static Map<String, ExpTomeDefinition> getTomes() {
        return Collections.unmodifiableMap(TOMES);
    }

    public static class ExpTomeDefinition {
        public final String name;
        public final String rarity;
        public final int maxLevels;
        public final EnchantmentCostConfig experiencePerLevel;

        public ExpTomeDefinition(String name, String rarity, int maxLevels, EnchantmentCostConfig experiencePerLevel) {
            this.name = name;
            this.rarity = rarity;
            this.maxLevels = maxLevels;
            this.experiencePerLevel = experiencePerLevel;
        }
    }
}
