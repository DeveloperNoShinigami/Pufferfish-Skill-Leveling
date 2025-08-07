package net.bluelotuscoding.puffishskillleveling.client.config.skill;

public record ClientSkillConnectionConfig(
		String skillAId,
		String skillBId,
		boolean bidirectional
) { }
