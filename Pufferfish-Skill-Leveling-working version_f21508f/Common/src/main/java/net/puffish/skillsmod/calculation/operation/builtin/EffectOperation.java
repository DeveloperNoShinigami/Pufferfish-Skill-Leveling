package net.puffish.skillsmod.calculation.operation.builtin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.calculation.operation.Operation;
import net.puffish.skillsmod.api.calculation.operation.OperationConfigContext;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.json.BuiltinJson;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.calculation.LegacyBuiltinPrototypes;
import net.puffish.skillsmod.util.LegacyUtils;

import java.util.ArrayList;
import java.util.Optional;

public class EffectOperation implements Operation<LivingEntity, StatusEffectInstance> {
	private final StatusEffect effect;

	private EffectOperation(StatusEffect effect) {
		this.effect = effect;
	}

	public static void register() {
		BuiltinPrototypes.LIVING_ENTITY.registerOperation(
				SkillsMod.createIdentifier("get_effect"),
				BuiltinPrototypes.STATUS_EFFECT_INSTANCE,
				EffectOperation::parse
		);

		LegacyBuiltinPrototypes.registerAlias(
				BuiltinPrototypes.LIVING_ENTITY,
				SkillsMod.createIdentifier("effect"),
				SkillsMod.createIdentifier("get_effect")
		);
	}

	public static Result<EffectOperation, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(LegacyUtils.wrapNoUnused(EffectOperation::parse, context));
	}

	public static Result<EffectOperation, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		var optEffect = rootObject.get("effect")
				.andThen(BuiltinJson::parseEffect)
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new EffectOperation(
					optEffect.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<StatusEffectInstance> apply(LivingEntity entity) {
		return Optional.ofNullable(entity.getStatusEffect(effect));
	}
}
