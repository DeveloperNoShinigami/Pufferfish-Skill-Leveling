package net.bluelotuscoding.puffishskillleveling.client.config.skill;

public record ClientSkillConfig(
		String id,
		int x,
		int y,
		String definitionId,
		boolean isRoot
) { }
