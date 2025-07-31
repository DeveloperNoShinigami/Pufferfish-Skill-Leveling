package net.puffish.skillsmod.client.config.skill;

public record ClientSkillConnectionConfig(
		String skillAId,
		String skillBId,
		boolean bidirectional
) { }
