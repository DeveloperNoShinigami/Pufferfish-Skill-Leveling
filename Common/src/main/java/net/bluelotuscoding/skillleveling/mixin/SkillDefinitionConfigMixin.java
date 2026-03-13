package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.util.AddonLogger;

import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.config.skill.SkillDefinitionConfig;
import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mixin(value = SkillDefinitionConfig.class, remap = false)
public abstract class SkillDefinitionConfigMixin {

    @Inject(method = "parse(Ljava/lang/String;Lnet/puffish/skillsmod/api/json/JsonObject;Lnet/puffish/skillsmod/api/config/ConfigContext;)Lnet/puffish/skillsmod/api/util/Result;", at = @At("HEAD"))
    private static void onParseHead(String id, JsonObject rootObject, ConfigContext context,
            CallbackInfoReturnable<Result<Optional<SkillDefinitionConfig>, Problem>> cir) {

        // We don't strictly enforce "type" presence here.
        // If type is missing, Pufferfish might use a default, or it might be implicit.
        // We still need to consume our custom fields to avoid "unused field" errors.

        // Consumed fields must be accessed to avoid "Unused field" errors in
        // Pufferfish.
        rootObject.get("type").getSuccess();
        rootObject.get("max_skill_level").getSuccess();
        rootObject.get("points_per_level").getSuccess();
        rootObject.get("merge_description").getSuccess();
        rootObject.get("descriptions").getSuccess();
        rootObject.get("extra_descriptions").getSuccess();
        rootObject.get("loot_mode").getSuccess();
        rootObject.get("category_id").getSuccess();
        rootObject.get("enchantment_cost").getSuccess();
        rootObject.get("imbuement_cost").getSuccess();
        rootObject.get("slot_opening_cost").getSuccess();
        rootObject.get("cleansing_cost").getSuccess();
        rootObject.get("hidden").getSuccess();
        rootObject.get("prerequisite_skills").getSuccess();
        rootObject.get("required_skill_for_level").getSuccess();
        rootObject.get("toggle").getSuccess();
        rootObject.get("keybind_slot").getSuccess();
        rootObject.get("cooldown").getSuccess();

        // Use the raw GSON object for injection
        var rawRoot = rootObject.getJson().getAsJsonObject();

        // Inject them into per_level_rewards reward data if they are missing there.
        // We do this for ALL skill types because any skill can contain this reward
        // type.
        rootObject.getArray("rewards").ifSuccess(rewardsArray -> {
            var rewards = rewardsArray.getJson();
            for (int i = 0; i < rewards.size(); i++) {
                var rewardElement = rewards.get(i);
                if (rewardElement.isJsonObject()) {
                    var rewardObj = rewardElement.getAsJsonObject();
                    var typeElement = rewardObj.get("type");
                    if (typeElement != null && typeElement.isJsonPrimitive()) {
                        String rewardType = typeElement.getAsString();

                        if (rewardType.equals("puffish_skill_leveling:per_level_rewards")) {
                            // Direct per_level_rewards at top level
                            addon$injectFieldsIntoPerLevelData(rewardObj, rawRoot, id);
                        } else if (rewardType.equals("puffish_skill_leveling:toggle")) {
                            // Toggle reward — look inside enable_rewards for nested per_level_rewards
                            var dataElement = rewardObj.get("data");
                            if (dataElement != null && dataElement.isJsonObject()) {
                                var toggleData = dataElement.getAsJsonObject();
                                addon$injectFieldsIntoNestedRewards(toggleData, "enable_rewards", rawRoot, id);
                                addon$injectFieldsIntoNestedRewards(toggleData, "disable_rewards", rawRoot, id);
                            }
                        }
                    }
                }
            }
        });
    }

    @Inject(method = "parse(Ljava/lang/String;Lnet/puffish/skillsmod/api/json/JsonObject;Lnet/puffish/skillsmod/api/config/ConfigContext;)Lnet/puffish/skillsmod/api/util/Result;", at = @At("RETURN"))
    private static void onParseReturn(String id, JsonObject rootObject, ConfigContext context,
            CallbackInfoReturnable<Result<Optional<SkillDefinitionConfig>, Problem>> cir) {

        // Always parse "hidden" and "loot_mode" for all skills (including standard
        // Pufferfish skills)
        final boolean isLootable = rootObject.get("loot_mode").getSuccess().isPresent();
        final boolean hidden = rootObject.get("hidden").getSuccess()
                .flatMap(e -> e.getAsBoolean().getSuccess())
                .orElse(false);

        Result<Optional<SkillDefinitionConfig>, Problem> result = cir.getReturnValue();
        if (result == null || result.getSuccess().isEmpty()) {
            return;
        }

        result.getSuccess().ifPresent(optConfig -> {
            optConfig.ifPresent(config -> {
                int points = rootObject.get("points_per_level").getSuccess()
                        .flatMap(e -> e.getAsInt().getSuccess())
                        .orElse(0);
                boolean merge = rootObject.get("merge_description").getSuccess()
                        .flatMap(e -> e.getAsBoolean().getSuccess()).orElse(false);

                List<LeveledConfigStorage.RequiredSkillEntry> requiredSkillsList = new ArrayList<>();

                // Parse "prerequisite_skills" (Pufferfish-like standard)
                var prereqsResult = rootObject.getArray("prerequisite_skills")
                        .getSuccess();

                prereqsResult.ifPresent(arr -> {
                    var jsonArr = arr.getJson();
                    for (int i = 0; i < jsonArr.size(); i++) {
                        var elem = jsonArr.get(i);
                        if (elem.isJsonObject()) {
                            var reqObj = elem.getAsJsonObject();

                            // Support both "skill" and "skill_id"
                            var skillIdElem = reqObj.has("skill") ? reqObj.get("skill")
                                    : reqObj.get("skill_id");

                            // Support "min_level", "level", and "max_level" (for backwards compatibility)
                            var minLevelElem = reqObj.has("min_level") ? reqObj.get("min_level")
                                    : (reqObj.has("level") ? reqObj.get("level")
                                            : reqObj.get("max_level"));

                            if (skillIdElem != null && !skillIdElem.isJsonNull() && minLevelElem != null
                                    && !minLevelElem.isJsonNull()) {
                                String reqSkillId = skillIdElem.getAsString();
                                int minLevel = minLevelElem.getAsInt();

                                // Parse optional category for cross-category support (support both category
                                // and category_id)
                                String category = reqObj.has("category")
                                        ? reqObj.get("category").getAsString()
                                        : (reqObj.has("category_id")
                                                ? reqObj.get("category_id").getAsString()
                                                : null);

                                requiredSkillsList.add(new LeveledConfigStorage.RequiredSkillEntry(
                                        reqSkillId, minLevel, category));
                            }
                        }
                    }
                });

                // Parse required_skill_for_level (level-gating prerequisites)
                Map<Integer, List<LeveledConfigStorage.RequiredSkillEntry>> levelPrereqs = new HashMap<>();
                rootObject.get("required_skill_for_level").getSuccess().ifPresent(levelReqElem -> {
                    var levelReqJson = levelReqElem.getJson();
                    if (levelReqJson.isJsonObject()) {
                        var levelReqObj = levelReqJson.getAsJsonObject();
                        for (var entry : levelReqObj.entrySet()) {
                            try {
                                int targetLevel = Integer.parseInt(entry.getKey());
                                List<LeveledConfigStorage.RequiredSkillEntry> prereqsForLevel = new ArrayList<>();
                                if (entry.getValue().isJsonArray()) {
                                    var prereqArr = entry.getValue().getAsJsonArray();
                                    for (int i = 0; i < prereqArr.size(); i++) {
                                        var prereqElem = prereqArr.get(i);
                                        if (prereqElem.isJsonObject()) {
                                            var prereqObj = prereqElem.getAsJsonObject();
                                            // Support both "skill" and "skill_id"
                                            var skillIdElem = prereqObj.has("skill")
                                                    ? prereqObj.get("skill")
                                                    : prereqObj.get("skill_id");

                                            var minLevelElem = prereqObj.has("min_level")
                                                    ? prereqObj.get("min_level")
                                                    : (prereqObj.has("level") ? prereqObj.get("level")
                                                            : prereqObj.get("max_level"));

                                            if (skillIdElem != null && !skillIdElem.isJsonNull()
                                                    && minLevelElem != null && !minLevelElem.isJsonNull()) {
                                                String reqSkillId = skillIdElem.getAsString();
                                                int minLevel = minLevelElem.getAsInt();
                                                String category = prereqObj.has("category")
                                                        ? prereqObj.get("category").getAsString()
                                                        : (prereqObj.has("category_id")
                                                                ? prereqObj.get("category_id").getAsString()
                                                                : null);
                                                prereqsForLevel.add(
                                                        new LeveledConfigStorage.RequiredSkillEntry(
                                                                reqSkillId, minLevel, category));
                                            }
                                        }
                                    }
                                }
                                if (!prereqsForLevel.isEmpty()) {
                                    levelPrereqs.put(targetLevel, prereqsForLevel);
                                }
                            } catch (NumberFormatException e) {
                                // Invalid level key, skip
                            }
                        }
                    }
                });

                String lootMode = rootObject.get("loot_mode").getSuccess()
                        .flatMap(e -> e.getAsString().getSuccess())
                        .orElse(null);

                String categoryId = rootObject.get("category_id").getSuccess()
                        .flatMap(e -> e.getAsString().getSuccess())
                        .orElse(null);

                // Parse enchantment cost options
                LeveledConfigStorage.EnchantmentCostConfig enchantmentCost = LeveledConfigStorage.EnchantmentCostConfig.FREE;
                var costResult = rootObject.get("enchantment_cost").getSuccess();

                if (costResult.isPresent()) {
                    var costElem = costResult.get().getJson();
                    if (costElem.isJsonPrimitive()) {
                        var primitive = costElem.getAsJsonPrimitive();
                        if (primitive.isNumber()) {
                            enchantmentCost = new LeveledConfigStorage.EnchantmentCostConfig(
                                    primitive.getAsInt());
                        } else if (primitive.isString()) {
                            enchantmentCost = new LeveledConfigStorage.EnchantmentCostConfig(
                                    primitive.getAsString());
                        }
                    } else if (costElem.isJsonArray()) {
                        var arrValue = costElem.getAsJsonArray();
                        int[] values = new int[arrValue.size()];
                        for (int i = 0; i < arrValue.size(); i++) {
                            values[i] = arrValue.get(i).getAsInt();
                        }
                        enchantmentCost = new LeveledConfigStorage.EnchantmentCostConfig(values);
                    } else if (costElem.isJsonObject()) {
                        var obj = costElem.getAsJsonObject();
                        if (obj.has("type") && obj.get("type").isJsonPrimitive()) {
                            String costType = obj.get("type").getAsString();
                            var dataObjResult = obj.get("data");
                            if (dataObjResult != null && dataObjResult.isJsonObject()) {
                                var dataObj = dataObjResult.getAsJsonObject();
                                if ("expression".equals(costType) && dataObj.has("expression")) {
                                    enchantmentCost = new LeveledConfigStorage.EnchantmentCostConfig(
                                            dataObj.get("expression").getAsString());
                                } else if ("array".equals(costType) && dataObj.has("values")) {
                                    var arrValue = dataObj.get("values").getAsJsonArray();
                                    int[] values = new int[arrValue.size()];
                                    for (int i = 0; i < arrValue.size(); i++) {
                                        values[i] = arrValue.get(i).getAsInt();
                                    }
                                    enchantmentCost = new LeveledConfigStorage.EnchantmentCostConfig(
                                            values);
                                }
                            }
                        }
                    }
                }

                // Parse imbuement cost options
                LeveledConfigStorage.EnchantmentCostConfig imbuementCost = null;
                var imbueResult = rootObject.get("imbuement_cost").getSuccess();

                if (imbueResult.isPresent()) {
                    imbuementCost = parseEnchantmentCost(imbueResult.get().getJson());
                }

                // Parse slot opening cost
                LeveledConfigStorage.EnchantmentCostConfig slotOpeningCost = LeveledConfigStorage.EnchantmentCostConfig.FREE;
                var slotOpeningResult = rootObject.get("slot_opening_cost").getSuccess();
                if (slotOpeningResult.isPresent()) {
                    slotOpeningCost = parseEnchantmentCost(slotOpeningResult.get().getJson());
                }

                // Parse cleansing cost
                LeveledConfigStorage.EnchantmentCostConfig cleansingCost = LeveledConfigStorage.EnchantmentCostConfig.FREE;
                var cleansingResult = rootObject.get("cleansing_cost").getSuccess();
                if (cleansingResult.isPresent()) {
                    cleansingCost = parseEnchantmentCost(cleansingResult.get().getJson());
                }

                // 1. Determine if we should register this skill in the addon storage
                boolean hasAddonFeatures = rootObject.get("max_skill_level").getSuccess().isPresent() ||
                        rootObject.get("points_per_level").getSuccess().isPresent() ||
                        rootObject.get("hidden").getSuccess().isPresent() ||
                        rootObject.get("loot_mode").getSuccess().isPresent() ||
                        rootObject.get("category_id").getSuccess().isPresent() ||
                        rootObject.get("enchantment_cost").getSuccess().isPresent() ||
                        rootObject.get("imbuement_cost").getSuccess().isPresent() ||
                        rootObject.get("slot_opening_cost").getSuccess().isPresent() ||
                        rootObject.get("cleansing_cost").getSuccess().isPresent() ||
                        rootObject.get("required_skill_for_level").getSuccess().isPresent() ||
                        rootObject.get("toggle").getSuccess().isPresent() ||
                        rootObject.get("keybind_slot").getSuccess().isPresent() ||
                        rootObject.get("cooldown").getSuccess().isPresent() ||
                        !requiredSkillsList.isEmpty();

                if (hasAddonFeatures) {
                    boolean toggle = rootObject.get("toggle").getSuccess()
                            .flatMap(e -> e.getAsBoolean().getSuccess())
                            .orElse(false);

                    // Default maxLevel: 0 for pure toggles (no purchasable levels), 1 for standard skills
                    int defaultMaxLevel = toggle ? 0 : 1;
                    int finalMaxLevels = rootObject.get("max_skill_level").getSuccess()
                            .flatMap(e -> e.getAsInt().getSuccess())
                            .orElse(defaultMaxLevel);
                    int keybindSlot = rootObject.get("keybind_slot").getSuccess()
                            .flatMap(e -> e.getAsInt().getSuccess())
                            .orElse(0);
                    int cooldown = rootObject.get("cooldown").getSuccess()
                            .flatMap(e -> e.getAsInt().getSuccess())
                            .orElse(0);

                    LeveledConfigStorage.put(id.toString(),
                            new LeveledConfigStorage.LeveledConfig(finalMaxLevels, points, merge,
                                    requiredSkillsList, levelPrereqs, lootMode, categoryId, enchantmentCost,
                                    imbuementCost,
                                    slotOpeningCost, cleansingCost, isLootable, hidden, toggle, keybindSlot, cooldown));

                    AddonLogger.LOGGER.info("[SkillDefinitionConfigMixin] Registered Addon Features for: " + id
                            + " (Toggle: " + toggle + ")");
                }
            });
        });
    }

    @Unique
    private static LeveledConfigStorage.EnchantmentCostConfig parseEnchantmentCost(
            com.google.gson.JsonElement json) {
        if (json.isJsonPrimitive()) {
            var primitive = json.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return new LeveledConfigStorage.EnchantmentCostConfig(primitive.getAsInt());
            } else if (primitive.isString()) {
                return new LeveledConfigStorage.EnchantmentCostConfig(primitive.getAsString());
            }
        } else if (json.isJsonArray()) {
            var arrValue = json.getAsJsonArray();
            int[] values = new int[arrValue.size()];
            for (int i = 0; i < arrValue.size(); i++) {
                values[i] = arrValue.get(i).getAsInt();
            }
            return new LeveledConfigStorage.EnchantmentCostConfig(values);
        } else if (json.isJsonObject()) {
            var obj = json.getAsJsonObject();
            if (obj.has("type") && obj.get("type").isJsonPrimitive()) {
                String costType = obj.get("type").getAsString();
                var dataObjResult = obj.get("data");
                if (dataObjResult != null && dataObjResult.isJsonObject()) {
                    var dataObj = dataObjResult.getAsJsonObject();
                    if ("expression".equals(costType) && dataObj.has("expression")) {
                        return new LeveledConfigStorage.EnchantmentCostConfig(
                                dataObj.get("expression").getAsString());
                    } else if ("array".equals(costType) && dataObj.has("values")) {
                        var arrValue = dataObj.get("values").getAsJsonArray();
                        int[] values = new int[arrValue.size()];
                        for (int i = 0; i < arrValue.size(); i++) {
                            values[i] = arrValue.get(i).getAsInt();
                        }
                        return new LeveledConfigStorage.EnchantmentCostConfig(values);
                    }
                }
            }
        }
        return LeveledConfigStorage.EnchantmentCostConfig.FREE;
    }

    /**
     * Inject top-level fields (descriptions, max_skill_level, etc.) into a
     * per_level_rewards data object.
     */
    @Unique
    private static void addon$injectFieldsIntoPerLevelData(com.google.gson.JsonObject rewardObj,
            com.google.gson.JsonObject rawRoot, String id) {
        var dataElement = rewardObj.get("data");
        if (dataElement == null || !dataElement.isJsonObject()) {
            return;
        }
        var data = dataElement.getAsJsonObject();

        // Inject missing fields from top level using raw GSON
        if (rawRoot.has("max_skill_level") && !data.has("max_skill_level")) {
            data.add("max_skill_level", rawRoot.get("max_skill_level"));
        }
        if (rawRoot.has("points_per_level") && !data.has("points_per_level")) {
            data.add("points_per_level", rawRoot.get("points_per_level"));
        }
        if (rawRoot.has("merge_description") && !data.has("merge_description")) {
            data.add("merge_description", rawRoot.get("merge_description"));
        }
        if (rawRoot.has("descriptions") && !data.has("descriptions")) {
            data.add("descriptions", rawRoot.get("descriptions"));
        }
        if (rawRoot.has("extra_descriptions") && !data.has("extra_descriptions")) {
            data.add("extra_descriptions", rawRoot.get("extra_descriptions"));
        }

        // Inject prerequisites into reward data
        if (rawRoot.has("prerequisite_skills") && !data.has("prerequisite_skills")) {
            data.add("prerequisite_skills", rawRoot.get("prerequisite_skills"));
        }
        if (rawRoot.has("required_skill_for_level") && !data.has("required_skill_for_level")) {
            data.add("required_skill_for_level", rawRoot.get("required_skill_for_level"));
        }

        // Ensure skill_id is set if missing
        if (!data.has("skill_id")) {
            data.addProperty("skill_id", id);
        }
    }

    /**
     * Search inside a toggle reward's sub-reward arrays (enable_rewards, disable_rewards)
     * for nested per_level_rewards and inject top-level fields into them.
     */
    @Unique
    private static void addon$injectFieldsIntoNestedRewards(com.google.gson.JsonObject toggleData,
            String arrayKey, com.google.gson.JsonObject rawRoot, String id) {
        var arrayElement = toggleData.get(arrayKey);
        if (arrayElement == null || !arrayElement.isJsonArray()) {
            return;
        }
        var nestedRewards = arrayElement.getAsJsonArray();
        for (int j = 0; j < nestedRewards.size(); j++) {
            var nestedElem = nestedRewards.get(j);
            if (nestedElem.isJsonObject()) {
                var nestedObj = nestedElem.getAsJsonObject();
                var nestedType = nestedObj.get("type");
                if (nestedType != null && nestedType.isJsonPrimitive()
                        && nestedType.getAsString().equals("puffish_skill_leveling:per_level_rewards")) {
                    addon$injectFieldsIntoPerLevelData(nestedObj, rawRoot, id);
                }
            }
        }
    }
}
