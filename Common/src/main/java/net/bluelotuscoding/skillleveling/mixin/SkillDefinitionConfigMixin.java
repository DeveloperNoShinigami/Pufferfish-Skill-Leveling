package net.bluelotuscoding.skillleveling.mixin;

import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.config.skill.SkillDefinitionConfig;
import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
import org.spongepowered.asm.mixin.Mixin;
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

                            LeveledConfigStorage.put(id,
                                    new LeveledConfigStorage.LeveledConfig(maxLevels, points, merge,
                                            requiredSkillsList, lootMode, categoryId));
                        });
            });
        });
    }
}
