package net.bluelotuscoding.puffishskillleveling.server.setup;

import net.bluelotuscoding.puffishskillleveling.SkillsMod;
import net.bluelotuscoding.puffishskillleveling.commands.arguments.CategoryArgumentType;
import net.bluelotuscoding.puffishskillleveling.commands.arguments.SkillArgumentType;

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
