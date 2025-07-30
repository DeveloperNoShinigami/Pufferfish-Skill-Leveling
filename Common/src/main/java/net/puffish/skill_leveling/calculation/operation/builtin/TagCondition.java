package net.puffish.skill_leveling.calculation.operation.builtin;

import net.minecraft.entity.Entity;
import net.puffish.skill_leveling.SkillsMod;
import net.puffish.skill_leveling.api.calculation.operation.Operation;
import net.puffish.skill_leveling.api.calculation.operation.OperationConfigContext;
import net.puffish.skill_leveling.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.json.JsonObject;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;
import net.puffish.skill_leveling.calculation.LegacyBuiltinPrototypes;
import net.puffish.skill_leveling.util.LegacyUtils;

import java.util.ArrayList;
import java.util.Optional;

public final class TagCondition implements Operation<Entity, Boolean> {
	private final String tag;

	private TagCondition(String tag) {
		this.tag = tag;
	}

	public static void register() {
		BuiltinPrototypes.ENTITY.registerOperation(
				SkillsMod.createIdentifier("has_tag"),
				BuiltinPrototypes.BOOLEAN,
				TagCondition::parse
		);

		LegacyBuiltinPrototypes.registerAlias(
				BuiltinPrototypes.ENTITY,
				SkillsMod.createIdentifier("tag"),
				SkillsMod.createIdentifier("has_tag")
		);
	}

	public static Result<TagCondition, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(LegacyUtils.wrapNoUnused(TagCondition::parse, context));
	}

	public static Result<TagCondition, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		var optTag = rootObject.getString("tag")
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new TagCondition(
					optTag.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<Boolean> apply(Entity entity) {
		return Optional.of(entity.getCommandTags().contains(tag));
	}
}
