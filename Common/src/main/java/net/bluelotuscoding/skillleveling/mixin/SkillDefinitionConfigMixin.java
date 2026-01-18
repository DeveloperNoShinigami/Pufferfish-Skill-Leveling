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

import java.util.Optional;

@Mixin(value = SkillDefinitionConfig.class, remap = false)
public abstract class SkillDefinitionConfigMixin {

    private static final String DEFAULT_TYPE = "puffish_skill_leveling:default";
    private static final String STACKABLE_TYPE = "puffish_skill_leveling:stackable";

    private static boolean isAddonType(String type) {
        return DEFAULT_TYPE.equals(type) || STACKABLE_TYPE.equals(type);
    }

    @Inject(method = "parse(Ljava/lang/String;Lnet/puffish/skillsmod/api/json/JsonObject;Lnet/puffish/skillsmod/api/config/ConfigContext;)Lnet/puffish/skillsmod/api/util/Result;", at = @At("HEAD"))
    private static void onParseHead(String id, JsonObject rootObject, ConfigContext context,
            CallbackInfoReturnable<Result<Optional<SkillDefinitionConfig>, Problem>> cir) {
        // Read the type field to consume it (prevents "unused field" error)
        // Only process leveling logic if this is a stackable skill
        var typeResult = rootObject.get("type").getSuccess();
        if (typeResult.isEmpty()) {
            // No type field - this is a regular Puffish skill, not a stackable one
            return;
        }

        var typeString = typeResult.flatMap(e -> e.getAsString().getSuccess());
        if (typeString.isEmpty() || !isAddonType(typeString.get())) {
            // Type doesn't match our addon's stackable type - skip leveling logic
            return;
        }

        // This is a stackable skill - read leveling parameters from root level
        var maxSkillLevel = rootObject.get("max_skill_level")
                .getSuccess()
                .or(() -> rootObject.get("max_levels").getSuccess());

        var pointsPerLevel = rootObject.get("points_per_level").getSuccess();
        var mergeDescription = rootObject.get("merge_description").getSuccess();
        var descriptions = rootObject.get("descriptions").getSuccess();
        var extraDescriptions = rootObject.get("extra_descriptions").getSuccess();
        var requiredSkill = rootObject.get("required_skill").getSuccess();

        // Inject them into per_level_rewards reward data if they are missing there
        rootObject.getArray("rewards").ifSuccess(rewardsArray -> {
            var jsonArray = rewardsArray.getJson();
            for (int i = 0; i < jsonArray.size(); i++) {
                var rewardElement = jsonArray.get(i);
                if (rewardElement.isJsonObject()) {
                    var rewardObj = rewardElement.getAsJsonObject();
                    var rewardTypeElement = rewardObj.get("type");
                    if (rewardTypeElement != null && rewardTypeElement.isJsonPrimitive()
                            && rewardTypeElement.getAsString().equals("puffish_skill_leveling:per_level_rewards")) {
                        var data = rewardObj.getAsJsonObject("data");
                        if (data != null) {
                            if (maxSkillLevel.isPresent() && !data.has("max_skill_level")) {
                                data.add("max_skill_level", maxSkillLevel.get().getJson());
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
        // Check if this is an addon skill before storing config
        var typeString = rootObject.get("type").getSuccess()
                .flatMap(e -> e.getAsString().getSuccess());
        if (typeString.isEmpty() || !isAddonType(typeString.get())) {
            // Not an addon skill - don't store in leveled config
            return;
        }

        Result<Optional<SkillDefinitionConfig>, Problem> result = cir.getReturnValue();
        result.getSuccess().ifPresent(optConfig -> {
            optConfig.ifPresent(config -> {
                // Store leveled config in external storage using skill ID as key
                rootObject.get("max_skill_level").getSuccess()
                        .or(() -> rootObject.get("max_levels").getSuccess())
                        .flatMap(e -> e.getAsInt().getSuccess())
                        .ifPresent(maxLevels -> {
                            int points = rootObject.get("points_per_level").getSuccess()
                                    .flatMap(e -> e.getAsInt().getSuccess())
                                    .orElse(0);
                            boolean merge = rootObject.get("merge_description").getSuccess()
                                    .flatMap(e -> e.getAsBoolean().getSuccess()).orElse(false);
                            LeveledConfigStorage.put(id,
                                    new LeveledConfigStorage.LeveledConfig(maxLevels, points, merge));
                        });
            });
        });
    }
}
