package net.puffish.skill_leveling.client.config.skill;

public record ClientSkillConfig(
		String id,
		int x,
		int y,
		String definitionId,
		boolean isRoot
) { }
