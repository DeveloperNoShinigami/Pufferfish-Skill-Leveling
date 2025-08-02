package net.puffish.skillsmod.config.skill;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.BuiltinJson;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.config.FrameConfig;
import net.puffish.skillsmod.config.IconConfig;
import net.puffish.skillsmod.util.DisposeContext;
import net.puffish.skillsmod.util.LegacyUtils;

import java.util.ArrayList;
import java.util.List;
import net.puffish.skillsmod.reward.builtin.PerLevelRewardsReward;

public record SkillDefinitionConfig(

                String id,
                Identifier type,
		int maxLevels,
		List<Text> descriptions,
		List<Text> extraDescriptions,
		Text title,
		IconConfig icon,
                FrameConfig frame,
                float size,
                boolean mergeDescription,

                List<SkillRewardConfig> rewards,
                int cost,
		int requiredSkills,
		int requiredPoints,
		int requiredSpentPoints,
		int requiredExclusions
) {

	public static Result<SkillDefinitionConfig, Problem> parse(String id, JsonElement rootElement, ConfigContext context) {
		return rootElement.getAsObject().andThen(
				LegacyUtils.wrapNoUnused(rootObject -> parse(id, rootObject, context), context)
		);
	}

	public static Result<SkillDefinitionConfig, Problem> parse(String id, JsonObject rootObject, ConfigContext context) {
		var problems = new ArrayList<Problem>();


                var optTitle = rootObject.get("title")
                                .andThen(BuiltinJson::parseText)
                                .ifFailure(problems::add)
                                .getSuccess();


                var type = rootObject.get("type")
                                .getSuccess() // ignore failure because this property is optional
                                .flatMap(element -> BuiltinJson.parseIdentifier(element)
                                                .ifFailure(problems::add)
                                                .getSuccess())
                                .orElse(Identifier.of("puffish_skills", "default"));

		var descriptions = rootObject.getArray("descriptions")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(array -> array.getAsList((i, e) -> BuiltinJson.parseText(e))
						.mapFailure(Problem::combine)
						.ifFailure(problems::add)
						.getSuccess())
				.orElseGet(() -> rootObject.get("description")
						.getSuccess() // ignore failure because this property is optional
						.flatMap(element -> BuiltinJson.parseText(element)
								.ifFailure(problems::add)
								.getSuccess())
						.map(List::of)
						.orElseGet(List::of));

		var extraDescriptions = rootObject.getArray("extra_descriptions")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(array -> array.getAsList((i, e) -> BuiltinJson.parseText(e))
						.mapFailure(Problem::combine)
						.ifFailure(problems::add)
						.getSuccess())
				.orElseGet(() -> rootObject.get("extra_description")
						.getSuccess() // ignore failure because this property is optional
						.flatMap(element -> BuiltinJson.parseText(element)
								.ifFailure(problems::add)
								.getSuccess())
						.map(List::of)
						.orElseGet(List::of));


		var optIcon = rootObject.get("icon")
				.andThen(element -> IconConfig.parse(element, context))
				.ifFailure(problems::add)
				.getSuccess();

		var frame = rootObject.get("frame")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> FrameConfig.parse(element, context)
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElseGet(FrameConfig::createDefault);

                var size = rootObject.get("size")
                                .getSuccess() // ignore failure because this property is optional
                                .flatMap(element -> element.getAsFloat()
                                                .ifFailure(problems::add)
                                                .getSuccess()
                                )
                                .orElse(1f);

                var mergeDescription = rootObject.get("merge_description")
                                .getSuccess() // ignore failure because this property is optional
                                .flatMap(element -> element.getAsBoolean()
                                                .ifFailure(problems::add)
                                                .getSuccess()
                                )
                                .orElse(false);

                var rewards = rootObject.getArray("rewards")
                                .getSuccess() // ignore failure because this property is optional
                                .flatMap(array -> array.getAsList((i, element) -> SkillRewardConfig.parse(element, context))
                                                .mapFailure(Problem::combine)
                                                .ifFailure(problems::add)
                                                .getSuccess())
                                .orElseGet(List::of);

                var maxLevelsElement = rootObject.get("max_skill_level").getSuccess()
                                .or(() -> rootObject.get("max_levels").getSuccess());

                var maxLevels = maxLevelsElement
                                .flatMap(element -> element.getAsInt()
                                                .ifFailure(problems::add)
                                                .getSuccess())
                                .orElseGet(() -> rewards.stream()
                                                .map(SkillRewardConfig::instance)
                                                .filter(PerLevelRewardsReward.class::isInstance)
                                                .map(PerLevelRewardsReward.class::cast)
                                                .filter(reward -> reward.getSkillId() == null || reward.getSkillId().equals(id))
                                                .mapToInt(PerLevelRewardsReward::getMaxLevel)
                                                .max()
                                                .orElse(1));

		var cost = rootObject.get("cost")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsInt()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(1);

		var requiredSkills = rootObject.get("required_skills")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsInt()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(1);

		var requiredPoints = rootObject.get("required_points")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsInt()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(0);

		var requiredSpentPoints = rootObject.get("required_spent_points")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsInt()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(0);

		var requiredExclusions = rootObject.get("required_exclusions")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsInt()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(1);

		// this field is generated be the editor, access it to avoid unused field error
		rootObject.get("metadata");


		if (problems.isEmpty()) {
                        return Result.success(new SkillDefinitionConfig(
                                        id,
                                        type,
                                        maxLevels,
					descriptions,
					extraDescriptions,
					optTitle.orElseThrow(),
					optIcon.orElseThrow(),
                                        frame,
                                        size,
                                        mergeDescription,
                                        rewards,
                                        cost,
                                        requiredSkills,
					requiredPoints,
					requiredSpentPoints,
					requiredExclusions
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}

	}

	public void dispose(DisposeContext context) {
		for (var reward : rewards) {
			reward.dispose(context);
		}
	}

}
