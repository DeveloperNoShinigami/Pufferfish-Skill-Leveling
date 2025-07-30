package net.puffish.skill_leveling.config;

import net.puffish.skill_leveling.SkillsMod;
import net.puffish.skill_leveling.api.config.ConfigContext;
import net.puffish.skill_leveling.api.json.BuiltinJson;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.json.JsonObject;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;
import net.puffish.skill_leveling.util.LegacyUtils;

import java.util.ArrayList;
import java.util.List;

public record ModConfig(int version, boolean showWarnings, List<String> categories) implements Config {

	public static Result<ModConfig, Problem> parse(JsonElement rootElement, ConfigContext context) {
		return rootElement.getAsObject().andThen(
				LegacyUtils.wrapNoUnusedConfig(rootObject -> parse(rootObject, context), context)
		);
	}

	private static Result<ModConfig, Problem> parse(JsonObject rootObject, ConfigContext context) {
		var problems = new ArrayList<Problem>();

		var optVersion = rootObject.getInt("version")
				.ifFailure(problems::add)
				.getSuccess();

		var showWarnings = rootObject.get("show_warnings")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsBoolean()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(false);

		var categories = rootObject.get("categories")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(categoriesElement -> categoriesElement.getAsArray()
						.andThen(array -> array.getAsList((i, element) -> BuiltinJson.parseIdentifierPath(element))
								.mapFailure(Problem::combine)
						)
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElseGet(List::of);

		if (problems.isEmpty()) {
			int version = optVersion.orElseThrow();

			if (version < SkillsMod.MIN_CONFIG_VERSION) {
				return Result.failure(Problem.message("Configuration is outdated. Check out the mod's wiki to learn how to update the configuration."));
			}
			if (version > SkillsMod.MAX_CONFIG_VERSION) {
				return Result.failure(Problem.message("Configuration is for a newer version of the mod. Please update the mod."));
			}
			if (version < SkillsMod.MAX_CONFIG_VERSION) {
				context.emitWarning(rootObject.getPath()
						.getObject("version")
						.createProblem("Configuration uses outdated version. Please update the configuration version to " + SkillsMod.MAX_CONFIG_VERSION)
						.toString()
				);
			}

			return Result.success(new ModConfig(
					version,
					showWarnings,
					categories
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

}
