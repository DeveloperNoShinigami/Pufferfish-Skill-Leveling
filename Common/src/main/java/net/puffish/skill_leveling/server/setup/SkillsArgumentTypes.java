package net.puffish.skill_leveling.server.setup;

import net.puffish.skill_leveling.SkillsMod;
import net.puffish.skill_leveling.commands.arguments.CategoryArgumentType;
import net.puffish.skill_leveling.commands.arguments.SkillArgumentType;

public class SkillsArgumentTypes {
	public static void register(ServerRegistrar registrar) {
		registrar.registerArgumentType(
				SkillsMod.createIdentifier("category"),
				CategoryArgumentType.class,
				new CategoryArgumentType.Serializer()
		);
		registrar.registerArgumentType(
				SkillsMod.createIdentifier("skill"),
				SkillArgumentType.class,
				new SkillArgumentType.Serializer()
		);
	}
}
