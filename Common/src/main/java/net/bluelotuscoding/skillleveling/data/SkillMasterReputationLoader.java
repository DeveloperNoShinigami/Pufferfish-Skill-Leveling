package net.bluelotuscoding.skillleveling.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.Map;

/**
 * Loads Skill Master reputation (experience) configuration from datapacks.
 * Path: data/puffish_skill_leveling/skill_master_reputation/config.json
 */
public class SkillMasterReputationLoader extends JsonDataLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Default configuration values
    public int baseExp = 5;
    public int bothModeBonus = 3;
    public int imbueOnlyBonus = 5;
    public int tierMultiplier = 2;

    public int masteryBonus = 2; // Extra exp per mastered skill
    public int maxMasteryBonus = 20; // Cap on mastery-based bonus

    public int sigilProxyExp = 5;
    public int tomeOfClearMindExp = 10;
    public int tomeOfCleansingExp = 15;
    public int sigilOfImbuementExp = 50;
    public int tomeOfCleansingUpgradeExp = 25;

    public SkillMasterReputationLoader() {
        super(GSON, "skill_master_reputation");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        // We only expect one config file: puffish_skill_leveling:config
        Identifier configId = new Identifier(SkillLevelingMod.MOD_ID, "config");
        JsonElement element = prepared.get(configId);

        if (element != null && element.isJsonObject()) {
            try {
                JsonObject json = element.getAsJsonObject();
                if (json.has("baseExp"))
                    baseExp = json.get("baseExp").getAsInt();
                if (json.has("bothModeBonus"))
                    bothModeBonus = json.get("bothModeBonus").getAsInt();
                if (json.has("imbueOnlyBonus"))
                    imbueOnlyBonus = json.get("imbueOnlyBonus").getAsInt();
                if (json.has("tierMultiplier"))
                    tierMultiplier = json.get("tierMultiplier").getAsInt();
                if (json.has("masteryBonus"))
                    masteryBonus = json.get("masteryBonus").getAsInt();
                if (json.has("maxMasteryBonus"))
                    maxMasteryBonus = json.get("maxMasteryBonus").getAsInt();

                if (json.has("staticExp")) {
                    JsonObject statics = json.getAsJsonObject("staticExp");
                    if (statics.has("sigilProxy"))
                        sigilProxyExp = statics.get("sigilProxy").getAsInt();
                    if (statics.has("tomeOfClearMind"))
                        tomeOfClearMindExp = statics.get("tomeOfClearMind").getAsInt();
                    if (statics.has("tomeOfCleansing"))
                        tomeOfCleansingExp = statics.get("tomeOfCleansing").getAsInt();
                    if (statics.has("sigilOfImbuement"))
                        sigilOfImbuementExp = statics.get("sigilOfImbuement").getAsInt();
                    if (statics.has("tomeOfCleansingUpgrade"))
                        tomeOfCleansingUpgradeExp = statics.get("tomeOfCleansingUpgrade").getAsInt();
                }

                SkillLevelingMod.getInstance().getLogger().info("Loaded Skill Master reputation config from datapack.");
            } catch (Exception e) {
                SkillLevelingMod.getInstance().getLogger()
                        .error("Error parsing Skill Master reputation config: " + e.getMessage());
            }
        }
    }

    public int calculateStandardExp(int tier, String lootMode, int masteryCount) {
        int exp = baseExp + (tier * tierMultiplier);

        // Apply mastery bonus
        int mBonus = Math.min(masteryCount * masteryBonus, maxMasteryBonus);
        exp += mBonus;

        if ("imbue_only".equals(lootMode)) {
            exp += imbueOnlyBonus;
        } else if ("both".equals(lootMode)) {
            exp += bothModeBonus;
        }
        return exp;
    }

    public int applyMasteryToStatic(int staticExp, int masteryCount) {
        int mBonus = Math.min(masteryCount * masteryBonus, maxMasteryBonus);
        return staticExp + mBonus;
    }
}
