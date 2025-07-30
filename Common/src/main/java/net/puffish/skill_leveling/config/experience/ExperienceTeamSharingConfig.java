package net.puffish.skill_leveling.config.experience;

import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.json.JsonObject;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;

import java.util.ArrayList;

public record ExperienceTeamSharingConfig(float distanceLimit) {

	public static Result<ExperienceTeamSharingConfig, Problem> parse(JsonElement rootElement) {
		return rootElement.getAsObject()
				.andThen(rootObject -> rootObject.noUnused(ExperienceTeamSharingConfig::parse));
	}

	public static Result<ExperienceTeamSharingConfig, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		var optDistanceLimit = rootObject.getFloat("distance_limit")
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new ExperienceTeamSharingConfig(
					optDistanceLimit.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

}
