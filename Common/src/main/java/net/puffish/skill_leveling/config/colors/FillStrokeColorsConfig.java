package net.puffish.skill_leveling.config.colors;

import net.puffish.skill_leveling.api.config.ConfigContext;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.json.JsonObject;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;
import net.puffish.skill_leveling.util.LegacyUtils;

import java.util.ArrayList;

public record FillStrokeColorsConfig(
		ColorConfig fill,
		ColorConfig stroke
) {
	public static Result<FillStrokeColorsConfig, Problem> parse(
			JsonElement rootElement,
			FillStrokeColorsConfig defaultColors,
			ConfigContext context
	) {
		return rootElement.getAsString().flatMap(
				string -> ColorConfig.parse(string, rootElement.getPath())
						.mapSuccess(fill -> new FillStrokeColorsConfig(fill, defaultColors.stroke)),
				failure -> rootElement.getAsObject()
						.andThen(LegacyUtils.wrapNoUnused(rootObject -> parse(rootObject, defaultColors), context))
		);
	}

	private static Result<FillStrokeColorsConfig, Problem> parse(
			JsonObject rootObject,
			FillStrokeColorsConfig defaultColors
	) {
		var problems = new ArrayList<Problem>();

		var fill = rootObject.get("fill")
				.getSuccess()
				.flatMap(element -> ColorConfig.parse(element)
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(defaultColors.fill);

		var stroke = rootObject.get("stroke")
				.getSuccess()
				.flatMap(element -> ColorConfig.parse(element)
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(defaultColors.stroke);

		if (problems.isEmpty()) {
			return Result.success(new FillStrokeColorsConfig(
					fill,
					stroke
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}
}
