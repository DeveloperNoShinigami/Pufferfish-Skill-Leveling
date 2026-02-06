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

    public int masteryBonus = 2;
    public int maxMasteryBonus = 20;

    public int sigilProxyExp = 5;
    public int tomeOfClearMindExp = 10;
    public int tomeOfCleansingExp = 15;
    public int sigilOfImbuementExp = 50;
    public int tomeOfCleansingUpgradeExp = 25;

    // New Dynamic Trade Settings
    public float upgradeTradeBaseChance = 0.1f;
    public float upgradeTradeMaxChance = 0.25f;
    public float tomePriceMultiplierLevel1 = 1.0f;
    public float tomePriceMultiplierMaxLevel = 0.5f;
    public float tomeUpgradePriceMultiplier = 0.6f;

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
                if (json.has("experience_settings")) {
                    JsonObject expSettings = json.getAsJsonObject("experience_settings");
                    if (expSettings.has("base_experience_per_trade"))
                        baseExp = expSettings.get("base_experience_per_trade").getAsInt();
                    if (expSettings.has("bonus_for_both_mode_skills"))
                        bothModeBonus = expSettings.get("bonus_for_both_mode_skills").getAsInt();
                    if (expSettings.has("bonus_for_imbue_only_skills"))
                        imbueOnlyBonus = expSettings.get("bonus_for_imbue_only_skills").getAsInt();
                    if (expSettings.has("experience_multiplier_per_villager_tier"))
                        tierMultiplier = expSettings.get("experience_multiplier_per_villager_tier").getAsInt();
                    if (expSettings.has("experience_bonus_per_mastered_skill"))
                        masteryBonus = expSettings.get("experience_bonus_per_mastered_skill").getAsInt();
                    if (expSettings.has("maximum_mastery_experience_bonus"))
                        maxMasteryBonus = expSettings.get("maximum_mastery_experience_bonus").getAsInt();
                }

                if (json.has("static_experience_rewards")) {
                    JsonObject statics = json.getAsJsonObject("static_experience_rewards");
                    if (statics.has("sigil_proxy_trade"))
                        sigilProxyExp = statics.get("sigil_proxy_trade").getAsInt();
                    if (statics.has("tome_of_clear_mind"))
                        tomeOfClearMindExp = statics.get("tome_of_clear_mind").getAsInt();
                    if (statics.has("tome_of_cleansing"))
                        tomeOfCleansingExp = statics.get("tome_of_cleansing").getAsInt();
                    if (statics.has("sigil_of_imbuement"))
                        sigilOfImbuementExp = statics.get("sigil_of_imbuement").getAsInt();
                    if (statics.has("tome_of_cleansing_upgrade"))
                        tomeOfCleansingUpgradeExp = statics.get("tome_of_cleansing_upgrade").getAsInt();
                }

                if (json.has("dynamic_trade_settings")) {
                    JsonObject dynamic = json.getAsJsonObject("dynamic_trade_settings");
                    if (dynamic.has("special_upgrade_trade_base_chance"))
                        upgradeTradeBaseChance = dynamic.get("special_upgrade_trade_base_chance").getAsFloat();
                    if (dynamic.has("special_upgrade_trade_max_chance"))
                        upgradeTradeMaxChance = dynamic.get("special_upgrade_trade_max_chance").getAsFloat();
                    if (dynamic.has("tome_price_multiplier_at_level_1"))
                        tomePriceMultiplierLevel1 = dynamic.get("tome_price_multiplier_at_level_1").getAsFloat();
                    if (dynamic.has("tome_price_multiplier_at_max_level"))
                        tomePriceMultiplierMaxLevel = dynamic.get("tome_price_multiplier_at_max_level").getAsFloat();
                    if (dynamic.has("tome_upgrade_price_multiplier"))
                        tomeUpgradePriceMultiplier = dynamic.get("tome_upgrade_price_multiplier").getAsFloat();
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

    public float calculateUpgradeChance(int masteryCount) {
        // Linear scaling based on mastery count (assuming 0-10 mastered skills for max
        // chance)
        float masteryProgress = Math.min(masteryCount / 10.0f, 1.0f);
        return upgradeTradeBaseChance + (masteryProgress * (upgradeTradeMaxChance - upgradeTradeBaseChance));
    }
}
