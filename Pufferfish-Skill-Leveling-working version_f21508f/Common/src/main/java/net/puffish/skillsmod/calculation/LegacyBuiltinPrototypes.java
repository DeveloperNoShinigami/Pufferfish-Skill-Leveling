package net.puffish.skillsmod.calculation;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.calculation.prototype.Prototype;
import net.puffish.skillsmod.impl.calculation.prototype.PrototypeImpl;

public final class LegacyBuiltinPrototypes {
	public static void register() {
		registerAlias(
				BuiltinPrototypes.WORLD,
				new Identifier("server"),
				new Identifier("get_server")
		);
		registerAlias(
				BuiltinPrototypes.WORLD,
				new Identifier("time_of_day"),
				new Identifier("get_time_of_day")
		);
		registerAlias(
				BuiltinPrototypes.ENTITY,
				new Identifier("type"),
				new Identifier("get_type")
		);
		registerAlias(
				BuiltinPrototypes.ENTITY,
				new Identifier("world"),
				new Identifier("get_world")
		);
		registerAlias(
				BuiltinPrototypes.LIVING_ENTITY,
				new Identifier("entity"),
				new Identifier("as_entity")
		);
		registerAlias(
				BuiltinPrototypes.LIVING_ENTITY,
				new Identifier("world"),
				new Identifier("get_world")
		);
		registerAlias(
				BuiltinPrototypes.LIVING_ENTITY,
				new Identifier("type"),
				new Identifier("get_type")
		);
		registerAlias(
				BuiltinPrototypes.LIVING_ENTITY,
				new Identifier("max_health"),
				new Identifier("get_max_health")
		);
		registerAlias(
				BuiltinPrototypes.LIVING_ENTITY,
				new Identifier("health"),
				new Identifier("get_health")
		);
		registerAlias(
				BuiltinPrototypes.PLAYER,
				new Identifier("living_entity"),
				new Identifier("as_living_entity")
		);
		registerAlias(
				BuiltinPrototypes.PLAYER,
				new Identifier("entity"),
				new Identifier("as_entity")
		);
		registerAlias(
				BuiltinPrototypes.PLAYER,
				new Identifier("world"),
				new Identifier("get_world")
		);
		registerAlias(
				BuiltinPrototypes.ITEM,
				new Identifier("saturation_modifier"),
				new Identifier("get_saturation_modifier")
		);
		registerAlias(
				BuiltinPrototypes.ITEM,
				new Identifier("nutrition"),
				new Identifier("get_nutrition")
		);
		registerAlias(
				BuiltinPrototypes.ITEM_STACK,
				new Identifier("item"),
				new Identifier("get_item")
		);
		registerAlias(
				BuiltinPrototypes.ITEM_STACK,
				new Identifier("count"),
				new Identifier("get_count")
		);
		registerAlias(
				BuiltinPrototypes.BLOCK,
				new Identifier("hardness"),
				new Identifier("get_hardness")
		);
		registerAlias(
				BuiltinPrototypes.BLOCK,
				new Identifier("blast_resistance"),
				new Identifier("get_blast_resistance")
		);
		registerAlias(
				BuiltinPrototypes.BLOCK_STATE,
				new Identifier("block"),
				new Identifier("get_block")
		);
		registerAlias(
				BuiltinPrototypes.DAMAGE_SOURCE,
				new Identifier("type"),
				new Identifier("get_type")
		);
		registerAlias(
				BuiltinPrototypes.DAMAGE_SOURCE,
				new Identifier("attacker"),
				new Identifier("get_attacker")
		);
		registerAlias(
				BuiltinPrototypes.DAMAGE_SOURCE,
				new Identifier("source"),
				new Identifier("get_source")
		);
		registerAlias(
				BuiltinPrototypes.STAT,
				new Identifier("type"),
				new Identifier("get_type")
		);
		registerAlias(
				BuiltinPrototypes.STATUS_EFFECT_INSTANCE,
				new Identifier("level"),
				new Identifier("get_level")
		);
		registerAlias(
				BuiltinPrototypes.STATUS_EFFECT_INSTANCE,
				new Identifier("duration"),
				new Identifier("get_duration")
		);
		registerAlias(
				BuiltinPrototypes.ENTITY_ATTRIBUTE_INSTANCE,
				new Identifier("value"),
				new Identifier("get_value")
		);
		registerAlias(
				BuiltinPrototypes.ENTITY_ATTRIBUTE_INSTANCE,
				new Identifier("base_value"),
				new Identifier("get_base_value")
		);
	}

	public static void registerAlias(Prototype<?> prototype, Identifier id, Identifier existingId) {
		if (prototype instanceof PrototypeImpl<?> impl) {
			impl.registerAlias(id, existingId);
		}
	}
}
