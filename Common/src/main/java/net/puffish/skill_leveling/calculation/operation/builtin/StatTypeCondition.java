package net.puffish.skill_leveling.calculation.operation.builtin;

import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.stat.StatType;
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

public class StatTypeCondition implements Operation<StatType<?>, Boolean> {

	private final RegistryEntryList<StatType<?>> statTypeEntries;

	private StatTypeCondition(RegistryEntryList<StatType<?>> statTypeEntries) {
		this.statTypeEntries = statTypeEntries;
	}

	public static void register() {
		BuiltinPrototypes.STAT_TYPE.registerOperation(
				SkillsMod.createIdentifier("test"),
				BuiltinPrototypes.BOOLEAN,
				StatTypeCondition::parse
		);
	}

	public static Result<StatTypeCondition, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(LegacyUtils.wrapNoUnused(StatTypeCondition::parse, context));
	}

	public static Result<StatTypeCondition, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		var optStatType = rootObject.get("stat")
				.andThen(BuiltinJson::parseStatTypeOrStatTypeTag)
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new StatTypeCondition(
					optStatType.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<Boolean> apply(StatType<?> statType) {
		return Optional.of(statTypeEntries.contains(Registries.STAT_TYPE.getEntry(statType)));
	}
}
