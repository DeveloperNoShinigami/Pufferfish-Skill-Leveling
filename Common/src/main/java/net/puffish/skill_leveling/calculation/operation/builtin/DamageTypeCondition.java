package net.puffish.skill_leveling.calculation.operation.builtin;

import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.entry.RegistryEntryList;
import net.puffish.skill_leveling.SkillsMod;
import net.puffish.skill_leveling.api.calculation.operation.Operation;
import net.puffish.skill_leveling.api.calculation.operation.OperationConfigContext;
import net.puffish.skill_leveling.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skill_leveling.api.config.ConfigContext;
import net.puffish.skill_leveling.api.json.BuiltinJson;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.json.JsonObject;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;
import net.puffish.skill_leveling.util.LegacyUtils;

import java.util.ArrayList;
import java.util.Optional;

public final class DamageTypeCondition implements Operation<DamageType, Boolean> {
	private final RegistryEntryList<DamageType> damageTypeEntries;

	private DamageTypeCondition(RegistryEntryList<DamageType> damageTypeEntries) {
		this.damageTypeEntries = damageTypeEntries;
	}

	public static void register() {
		BuiltinPrototypes.DAMAGE_TYPE.registerOperation(
				SkillsMod.createIdentifier("test"),
				BuiltinPrototypes.BOOLEAN,
				DamageTypeCondition::parse
		);
	}

	public static Result<DamageTypeCondition, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(LegacyUtils.wrapNoUnused(rootObject -> parse(rootObject, context), context));
	}

	public static Result<DamageTypeCondition, Problem> parse(JsonObject rootObject, ConfigContext context) {
		var problems = new ArrayList<Problem>();

		var optDamageType = rootObject.get("damage_type")
				.orElse(LegacyUtils.wrapDeprecated(
						() -> rootObject.get("damage"),
						3,
						context
				))
				.andThen(damageElement -> BuiltinJson.parseDamageTypeOrDamageTypeTag(damageElement, context.getServer().getRegistryManager()))
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new DamageTypeCondition(
					optDamageType.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<Boolean> apply(DamageType damageType) {
		return Optional.of(
				damageTypeEntries.stream().anyMatch(entry -> entry.value() == damageType)
		);
	}
}
