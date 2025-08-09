package net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin;

import net.minecraft.entity.Entity;
import net.bluelotuscoding.puffishskillleveling.SkillsMod;
import net.bluelotuscoding.puffishskillleveling.api.calculation.operation.Operation;
import net.bluelotuscoding.puffishskillleveling.api.calculation.operation.OperationConfigContext;
import net.bluelotuscoding.puffishskillleveling.api.calculation.prototype.BuiltinPrototypes;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonElement;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonObject;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;
import net.bluelotuscoding.puffishskillleveling.calculation.LegacyBuiltinPrototypes;
import net.bluelotuscoding.puffishskillleveling.util.LegacyUtils;

import java.util.ArrayList;
import java.util.Optional;

public final class ScoreboardOperation implements Operation<Entity, Double> {
	private final String objectiveName;

	private ScoreboardOperation(String objectiveName) {
		this.objectiveName = objectiveName;
	}

	public static void register() {
		BuiltinPrototypes.ENTITY.registerOperation(
				SkillsMod.createIdentifier("get_score"),
				BuiltinPrototypes.NUMBER,
				ScoreboardOperation::parse
		);

		LegacyBuiltinPrototypes.registerAlias(
				BuiltinPrototypes.ENTITY,
				SkillsMod.createIdentifier("scoreboard"),
				SkillsMod.createIdentifier("get_score")
		);
	}

	public static Result<ScoreboardOperation, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(LegacyUtils.wrapNoUnused(ScoreboardOperation::parse, context));
	}

	public static Result<ScoreboardOperation, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		var optScoreboard = rootObject.getString("objective")
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new ScoreboardOperation(
					optScoreboard.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<Double> apply(Entity entity) {
		var scoreboard = entity.getWorld().getScoreboard();
		return Optional.ofNullable(scoreboard.getNullableObjective(objectiveName))
				.map(objective -> Optional.ofNullable(scoreboard.getPlayerObjectives(entity.getEntityName()).get(objective))
						.map(score -> (double) score.getScore())
						.orElse(0.0));
	}
}
