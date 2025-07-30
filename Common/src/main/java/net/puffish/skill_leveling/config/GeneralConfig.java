package net.puffish.skill_leveling.config;

import net.minecraft.text.Text;
import net.puffish.skill_leveling.api.config.ConfigContext;
import net.puffish.skill_leveling.api.json.BuiltinJson;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.json.JsonObject;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;
import net.puffish.skill_leveling.config.colors.ColorsConfig;
import net.puffish.skill_leveling.util.LegacyUtils;

import java.util.ArrayList;

public record GeneralConfig(
		Text title,
		IconConfig icon,
		BackgroundConfig background,
		ColorsConfig colors,
		boolean unlockedByDefault,
		int startingPoints,
		boolean exclusiveRoot,
		int spentPointsLimit
) {

	public static Result<GeneralConfig, Problem> parse(JsonElement rootElement, ConfigContext context) {
		return rootElement.getAsObject()
				.andThen(LegacyUtils.wrapNoUnused(rootObject -> parse(rootObject, context), context));
	}

	public static Result<GeneralConfig, Problem> parse(JsonObject rootObject, ConfigContext context) {
		var problems = new ArrayList<Problem>();

		var optTitle = rootObject.get("title")
				.andThen(BuiltinJson::parseText)
				.ifFailure(problems::add)
				.getSuccess();

		var optIcon = rootObject.get("icon")
				.andThen(element -> IconConfig.parse(element, context))
				.ifFailure(problems::add)
				.getSuccess();

		var optBackground = rootObject.get("background")
				.andThen(element -> BackgroundConfig.parse(element, context))
				.ifFailure(problems::add)
				.getSuccess();

		var colors = rootObject.get("colors")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> ColorsConfig.parse(element, context)
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElseGet(ColorsConfig::createDefault);

		var unlockedByDefault = rootObject.get("unlocked_by_default")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsBoolean()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(true);

		var startingPoints = rootObject.get("starting_points")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsInt()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(0);

		var exclusiveRoot = rootObject.get("exclusive_root")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsBoolean()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(false);

		var spentPointsLimit = rootObject.get("spent_points_limit")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsInt()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(Integer.MAX_VALUE);

		if (problems.isEmpty()) {
			return Result.success(new GeneralConfig(
					optTitle.orElseThrow(),
					optIcon.orElseThrow(),
					optBackground.orElseThrow(),
					colors,
					unlockedByDefault,
					startingPoints,
					exclusiveRoot,
					spentPointsLimit
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

}
