package net.puffish.skill_leveling.calculation.operation;

import net.puffish.skill_leveling.calculation.operation.builtin.AttributeOperation;
import net.puffish.skill_leveling.calculation.operation.builtin.BlockCondition;
import net.puffish.skill_leveling.calculation.operation.builtin.BlockStateCondition;
import net.puffish.skill_leveling.calculation.operation.builtin.DamageSourceClassification;
import net.puffish.skill_leveling.calculation.operation.builtin.DamageTypeCondition;
import net.puffish.skill_leveling.calculation.operation.builtin.EffectOperation;
import net.puffish.skill_leveling.calculation.operation.builtin.EntityTypeCondition;
import net.puffish.skill_leveling.calculation.operation.builtin.ItemCondition;
import net.puffish.skill_leveling.calculation.operation.builtin.ItemStackCondition;
import net.puffish.skill_leveling.calculation.operation.builtin.ScoreboardOperation;
import net.puffish.skill_leveling.calculation.operation.builtin.StatCondition;
import net.puffish.skill_leveling.calculation.operation.builtin.StatTypeCondition;
import net.puffish.skill_leveling.calculation.operation.builtin.StatValueOperation;
import net.puffish.skill_leveling.calculation.operation.builtin.SwitchOperation;
import net.puffish.skill_leveling.calculation.operation.builtin.TagCondition;
import net.puffish.skill_leveling.calculation.operation.builtin.WorldCondition;
import net.puffish.skill_leveling.calculation.operation.builtin.legacy.LegacyBlockTagCondition;
import net.puffish.skill_leveling.calculation.operation.builtin.legacy.LegacyDamageTypeTagCondition;
import net.puffish.skill_leveling.calculation.operation.builtin.legacy.LegacyEntityTypeTagCondition;
import net.puffish.skill_leveling.calculation.operation.builtin.legacy.LegacyItemTagCondition;

public class BuiltinOperations {
	public static void register() {
		AttributeOperation.register();
		BlockCondition.register();
		BlockStateCondition.register();
		DamageSourceClassification.register();
		DamageTypeCondition.register();
		EffectOperation.register();
		EntityTypeCondition.register();
		ItemCondition.register();
		ItemStackCondition.register();
		ScoreboardOperation.register();
		StatCondition.register();
		StatTypeCondition.register();
		StatValueOperation.register();
		SwitchOperation.register();
		TagCondition.register();
		WorldCondition.register();

		LegacyBlockTagCondition.register();
		LegacyDamageTypeTagCondition.register();
		LegacyEntityTypeTagCondition.register();
		LegacyItemTagCondition.register();
	}
}
