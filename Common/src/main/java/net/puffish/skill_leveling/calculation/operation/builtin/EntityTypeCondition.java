package net.puffish.skill_leveling.calculation.operation.builtin;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.entry.RegistryEntryList;
import net.puffish.skill_leveling.SkillsMod;
import net.puffish.skill_leveling.api.calculation.operation.Operation;
import net.puffish.skill_leveling.api.calculation.operation.OperationConfigContext;
import net.puffish.skill_leveling.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skill_leveling.api.json.BuiltinJson;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.json.JsonObject;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;
import net.puffish.skill_leveling.util.LegacyUtils;

import java.util.ArrayList;
import java.util.Optional;

public final class EntityTypeCondition implements Operation<EntityType<?>, Boolean> {
	private final RegistryEntryList<EntityType<?>> entityTypeEntries;

	private EntityTypeCondition(RegistryEntryList<EntityType<?>> entityTypeEntries) {
		this.entityTypeEntries = entityTypeEntries;
	}

	public static void register() {
		BuiltinPrototypes.ENTITY_TYPE.registerOperation(
				SkillsMod.createIdentifier("test"),
				BuiltinPrototypes.BOOLEAN,
				EntityTypeCondition::parse
		);
	}

	public static Result<EntityTypeCondition, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(LegacyUtils.wrapNoUnused(rootObject -> parse(rootObject, context), context));
	}

	public static Result<EntityTypeCondition, Problem> parse(JsonObject rootObject, OperationConfigContext context) {
		var problems = new ArrayList<Problem>();

		var optEntityType = rootObject.get("entity_type")
				.orElse(LegacyUtils.wrapDeprecated(
						() -> rootObject.get("entity"),
						3,
						context
				))
				.andThen(BuiltinJson::parseEntityTypeOrEntityTypeTag)
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new EntityTypeCondition(
					optEntityType.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<Boolean> apply(EntityType<?> entityType) {
		return Optional.of(entityTypeEntries.contains(entityType.getRegistryEntry()));
	}
}
