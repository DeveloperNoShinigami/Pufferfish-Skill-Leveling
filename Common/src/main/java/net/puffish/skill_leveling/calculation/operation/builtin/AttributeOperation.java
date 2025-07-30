package net.puffish.skill_leveling.calculation.operation.builtin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.puffish.skill_leveling.SkillsMod;
import net.puffish.skill_leveling.api.calculation.operation.Operation;
import net.puffish.skill_leveling.api.calculation.operation.OperationConfigContext;
import net.puffish.skill_leveling.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skill_leveling.api.json.BuiltinJson;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.json.JsonObject;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;
import net.puffish.skill_leveling.calculation.LegacyBuiltinPrototypes;
import net.puffish.skill_leveling.util.LegacyUtils;

import java.util.ArrayList;
import java.util.Optional;

public class AttributeOperation implements Operation<LivingEntity, EntityAttributeInstance> {
	private final EntityAttribute attribute;

	private AttributeOperation(EntityAttribute attribute) {
		this.attribute = attribute;
	}

	public static void register() {
		BuiltinPrototypes.LIVING_ENTITY.registerOperation(
				SkillsMod.createIdentifier("get_attribute"),
				BuiltinPrototypes.ENTITY_ATTRIBUTE_INSTANCE,
				AttributeOperation::parse
		);

		LegacyBuiltinPrototypes.registerAlias(
				BuiltinPrototypes.LIVING_ENTITY,
				SkillsMod.createIdentifier("attribute"),
				SkillsMod.createIdentifier("get_attribute")
		);
	}

	public static Result<AttributeOperation, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(LegacyUtils.wrapNoUnused(AttributeOperation::parse, context));
	}

	public static Result<AttributeOperation, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		var optAttribute = rootObject.get("attribute")
				.andThen(BuiltinJson::parseAttribute)
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new AttributeOperation(
					optAttribute.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<EntityAttributeInstance> apply(LivingEntity entity) {
		return Optional.ofNullable(entity.getAttributeInstance(attribute));
	}
}
