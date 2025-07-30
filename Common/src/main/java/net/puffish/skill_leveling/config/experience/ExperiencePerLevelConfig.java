package net.puffish.skill_leveling.config.experience;

import net.puffish.skill_leveling.SkillsMod;
import net.puffish.skill_leveling.api.config.ConfigContext;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.json.JsonObject;
import net.puffish.skill_leveling.api.json.JsonPath;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;
import net.puffish.skill_leveling.expression.DefaultParser;
import net.puffish.skill_leveling.util.LegacyUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public record ExperiencePerLevelConfig(
		Function<Integer, Integer> function
) {

	public static Result<ExperiencePerLevelConfig, Problem> parse(JsonElement rootElement, ConfigContext context) {
		return rootElement.getAsObject().andThen(
				LegacyUtils.wrapNoUnused(rootObject -> parse(rootObject, context), context)
		);
	}

	public static Result<ExperiencePerLevelConfig, Problem> parse(JsonObject rootObject, ConfigContext context) {
		var problems = new ArrayList<Problem>();

		var optTypeElement = rootObject.get("type")
				.ifFailure(problems::add)
				.getSuccess();

		var optType = optTypeElement.flatMap(
				typeElement -> typeElement.getAsString()
						.ifFailure(problems::add)
						.getSuccess()
		);

		var optData = rootObject.get("data")
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return build(
					optType.orElseThrow(),
					optData.orElseThrow(),
					optTypeElement.orElseThrow().getPath(),
					context
			);
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	private static Result<ExperiencePerLevelConfig, Problem> build(String type, JsonElement dataElement, JsonPath typeElementPath, ConfigContext context) {
		return switch (type) {
			case "expression" -> parseExpression(dataElement, context);
			case "values" -> parseValues(dataElement, context);
			default -> Result.failure(typeElementPath.createProblem("Expected a valid experience per level type"));
		};
	}

	private static Result<ExperiencePerLevelConfig, Problem> parseExpression(JsonElement rootElement, ConfigContext context) {
		return rootElement.getAsObject().andThen(
				LegacyUtils.wrapNoUnused(rootObject -> parseExpression(rootObject, context), context)
		);
	}

	private static Result<ExperiencePerLevelConfig, Problem> parseExpression(JsonObject rootObject, ConfigContext context) {
		var problems = new ArrayList<Problem>();

		var optExpressionElement = rootObject.get("expression")
				.ifFailure(problems::add)
				.getSuccess();

		var optExpression = optExpressionElement.flatMap(
				expressionElement -> expressionElement.getAsString()
						.andThen(expressionString -> DefaultParser.parse(expressionString, Set.of("level")))
						.ifFailure(problems::add)
						.getSuccess()
		);

		if (problems.isEmpty()) {
			var expressionElement = optExpressionElement.orElseThrow();
			var expression = optExpression.orElseThrow();

			return Result.success(new ExperiencePerLevelConfig(
					level -> {
						var value = expression.eval(Map.ofEntries(Map.entry("level", (double) level)));
						if (Double.isFinite(value)) {
							return (int) Math.round(value);
						} else {
							SkillsMod.getInstance().getLogger().warn(
									expressionElement.getPath()
											.createProblem("Expression returned a value that is not finite")
											.toString()
							);
							return 0;
						}
					}
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	private static Result<ExperiencePerLevelConfig, Problem> parseValues(JsonElement rootElement, ConfigContext context) {
		return rootElement.getAsObject().andThen(
				LegacyUtils.wrapNoUnused(rootObject -> parseValues(rootObject, context), context)
		);
	}

	private static Result<ExperiencePerLevelConfig, Problem> parseValues(JsonObject rootObject, ConfigContext context) {
		var problems = new ArrayList<Problem>();

		var optValues = rootObject.getArray("values")
				.andThen(array -> array.getAsList((i, element) -> element.getAsInt())
						.mapFailure(Problem::combine)
				)
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			var values = optValues.orElseThrow();

			return Result.success(new ExperiencePerLevelConfig(
					level -> values.get(Math.min(level, values.size() - 1))
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

}
