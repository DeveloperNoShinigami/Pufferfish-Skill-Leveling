package net.bluelotuscoding.skillleveling.mixin;

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
import java.util.List;
import java.util.Optional;

@Mixin(value = SkillDefinitionConfig.class, remap = false)
public abstract class SkillDefinitionConfigMixin {

    private static final String DEFAULT_TYPE = "puffish_skill_leveling:default";
    private static final String STACKABLE_TYPE = "puffish_skill_leveling:stackable";

    private static final String PUFFISH_DEFAULT_TYPE = "puffish_skills:default";
    private static final String PUFFISH_STACKABLE_TYPE = "puffish_skills:stackable";

    private static boolean isAddonType(String type) {
        return DEFAULT_TYPE.equals(type) || STACKABLE_TYPE.equals(type)
                || PUFFISH_DEFAULT_TYPE.equals(type) || PUFFISH_STACKABLE_TYPE.equals(type);
    }

    @Inject(method = "parse(Ljava/lang/String;Lnet/puffish/skillsmod/api/json/JsonObject;Lnet/puffish/skillsmod/api/config/ConfigContext;)Lnet/puffish/skillsmod/api/util/Result;", at = @At("HEAD"))
    private static void onParseHead(String id, JsonObject rootObject, ConfigContext context,
            CallbackInfoReturnable<Result<Optional<SkillDefinitionConfig>, Problem>> cir) {

        var typeResult = rootObject.get("type").getSuccess();
        if (typeResult.isEmpty()) {
            return;
        }

        var typeString = typeResult.flatMap(e -> e.getAsString().getSuccess());
        if (typeString.isEmpty() || !isAddonType(typeString.get())) {
            return;
        }

        // Consume our fields to prevent "unused field" errors from Pufferfish
        var maxSkillLevel = rootObject.get("max_skill_level").getSuccess();
        var maxLevels = rootObject.get("max_levels").getSuccess();
        var pointsPerLevel = rootObject.get("points_per_level").getSuccess();
        var mergeDescription = rootObject.get("merge_description").getSuccess();
        var descriptions = rootObject.get("descriptions").getSuccess();
        var extraDescriptions = rootObject.get("extra_descriptions").getSuccess();
        var requiredSkill = rootObject.get("required_skill").getSuccess();
        // Use getArray to properly consume the prerequisite_skills field
        rootObject.getArray("prerequisite_skills");
        rootObject.get("loot_mode");
        rootObject.get("category_id");
        rootObject.get("enchantment_levels");
        rootObject.get("enchantment_cost");
        rootObject.get("imbuement_levels");
        rootObject.get("imbuement_cost");
        rootObject.get("slot_opening_cost");
        rootObject.get("cleansing_cost");

        // Inject them into per_level_rewards reward data if they are missing there
        rootObject.getArray("rewards").ifSuccess(rewardsArray -> {
            var rewards = rewardsArray.getJson();
            for (int i = 0; i < rewards.size(); i++) {
                var rewardElement = rewards.get(i);
                if (rewardElement.isJsonObject()) {
                    var rewardObj = rewardElement.getAsJsonObject();
                    var typeElement = rewardObj.get("type");
                    if (typeElement != null && typeElement.isJsonPrimitive()
                            && typeElement.getAsString().equals("puffish_skill_leveling:per_level_rewards")) {
                        var dataElement = rewardObj.get("data");
                        if (dataElement != null && dataElement.isJsonObject()) {
                            var data = dataElement.getAsJsonObject();

                            // Inject missing fields from top level
                            if (maxSkillLevel.isPresent() && !data.has("max_skill_level")) {
                                data.add("max_skill_level", maxSkillLevel.get().getJson());
                            }
                            if (maxLevels.isPresent() && !data.has("max_levels")) {
                                data.add("max_levels", maxLevels.get().getJson());
                            }
                            if (pointsPerLevel.isPresent() && !data.has("points_per_level")) {
                                data.add("points_per_level", pointsPerLevel.get().getJson());
                            }
                            if (mergeDescription.isPresent() && !data.has("merge_description")) {
                                data.add("merge_description", mergeDescription.get().getJson());
                            }
                            if (descriptions.isPresent() && !data.has("descriptions")) {
                                data.add("descriptions", descriptions.get().getJson());
                            }
                            if (extraDescriptions.isPresent() && !data.has("extra_descriptions")) {
                                data.add("extra_descriptions", extraDescriptions.get().getJson());
                            }
                            if (requiredSkill.isPresent() && !data.has("required_skill")) {
                                data.add("required_skill", requiredSkill.get().getJson());
                            }

                            // Ensure skill_id is set if missing
                            if (!data.has("skill_id")) {
                                data.addProperty("skill_id", id);
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

        var typeResult = rootObject.get("type").getSuccess();
        if (typeResult.isEmpty()) {
            return;
        }

        var typeString = typeResult.flatMap(e -> e.getAsString().getSuccess());
        if (typeString.isEmpty() || !isAddonType(typeString.get())) {
            return;
        }

        Result<Optional<SkillDefinitionConfig>, Problem> result = cir.getReturnValue();
        result.getSuccess().ifPresent(optConfig -> {
            optConfig.ifPresent(config -> {
                rootObject.get("max_skill_level").getSuccess()
                        .or(() -> rootObject.get("max_levels").getSuccess())
                        .flatMap(e -> e.getAsInt().getSuccess())
                        .ifPresent(maxLevels -> {
                            int points = rootObject.get("points_per_level").getSuccess()
                                    .flatMap(e -> e.getAsInt().getSuccess())
                                    .orElse(0);
                            boolean merge = rootObject.get("merge_description").getSuccess()
                                    .flatMap(e -> e.getAsBoolean().getSuccess()).orElse(false);

                            List<LeveledConfigStorage.RequiredSkillEntry> requiredSkillsList = new ArrayList<>();
                            rootObject.getArray("prerequisite_skills").ifSuccess(arr -> {
                                var jsonArr = arr.getJson();
                                for (int i = 0; i < jsonArr.size(); i++) {
                                    var elem = jsonArr.get(i);
                                    if (elem.isJsonObject()) {
                                        var reqObj = elem.getAsJsonObject();
                                        var skillIdElem = reqObj.get("skill");
                                        var minLevelElem = reqObj.get("min_level");
                                        if (skillIdElem != null && minLevelElem != null) {
                                            String reqSkillId = skillIdElem.getAsString();
                                            int minLevel = minLevelElem.getAsInt();
                                            requiredSkillsList.add(
                                                    new LeveledConfigStorage.RequiredSkillEntry(reqSkillId, minLevel));
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

                            // Parse enchantment cost options (supports enchantment_cost and legacy
                            // enchantment_levels)
                            LeveledConfigStorage.EnchantmentCostConfig enchantmentCost = LeveledConfigStorage.EnchantmentCostConfig.FREE;
                            var costResult = rootObject.get("enchantment_cost").getSuccess()
                                    .or(() -> rootObject.get("enchantment_levels").getSuccess());

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
                            var imbueResult = rootObject.get("imbuement_cost").getSuccess()
                                    .or(() -> rootObject.get("imbuement_levels").getSuccess());

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

                            LeveledConfigStorage.put(id,
                                    new LeveledConfigStorage.LeveledConfig(maxLevels, points, merge,
                                            requiredSkillsList, lootMode, categoryId, enchantmentCost, imbuementCost,
                                            slotOpeningCost, cleansingCost));
                        });
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
}
