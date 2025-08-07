package net.bluelotuscoding.puffishskillleveling.calculation.operation;

import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.AttributeOperation;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.BlockCondition;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.BlockStateCondition;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.DamageSourceClassification;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.DamageTypeCondition;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.EffectOperation;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.EntityTypeCondition;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.ItemCondition;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.ItemStackCondition;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.ScoreboardOperation;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.StatCondition;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.StatTypeCondition;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.StatValueOperation;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.SwitchOperation;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.TagCondition;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.WorldCondition;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.legacy.LegacyBlockTagCondition;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.legacy.LegacyDamageTypeTagCondition;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.legacy.LegacyEntityTypeTagCondition;
import net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin.legacy.LegacyItemTagCondition;

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
