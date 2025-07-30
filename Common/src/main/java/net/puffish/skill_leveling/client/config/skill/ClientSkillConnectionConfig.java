package net.puffish.skill_leveling.client.config.skill;

public record ClientSkillConnectionConfig(
		String skillAId,
		String skillBId,
		boolean bidirectional
) { }
