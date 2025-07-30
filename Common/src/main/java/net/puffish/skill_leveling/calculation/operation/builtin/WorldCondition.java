package net.puffish.skill_leveling.calculation.operation.builtin;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.puffish.skill_leveling.SkillsMod;
import net.puffish.skill_leveling.api.calculation.operation.Operation;
import net.puffish.skill_leveling.api.calculation.operation.OperationConfigContext;
import net.puffish.skill_leveling.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skill_leveling.api.json.BuiltinJson;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.json.JsonObject;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;

import java.util.ArrayList;
import java.util.Optional;

public final class WorldCondition implements Operation<ServerWorld, Boolean> {
	private final Identifier dimension;

	private WorldCondition(Identifier dimension) {
		this.dimension = dimension;
	}

	public static void register() {
		BuiltinPrototypes.WORLD.registerOperation(
				SkillsMod.createIdentifier("test"),
				BuiltinPrototypes.BOOLEAN,
				WorldCondition::parse
		);
	}

	public static Result<WorldCondition, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(rootObject -> rootObject.noUnused(WorldCondition::parse));
	}

	public static Result<WorldCondition, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		var optDimension = rootObject.get("dimension")
				.andThen(BuiltinJson::parseIdentifier)
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new WorldCondition(
					optDimension.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<Boolean> apply(ServerWorld world) {
		return Optional.of(world.getRegistryKey().getValue().equals(dimension));
	}
}
