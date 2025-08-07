package net.bluelotuscoding.puffishskillleveling.client.config.skill;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.client.config.ClientFrameConfig;
import net.bluelotuscoding.puffishskillleveling.client.config.ClientIconConfig;

public record ClientSkillDefinitionConfig(
		String id,
		Identifier type,
		int maxLevels,
		java.util.List<Text> descriptions,
		java.util.List<Text> extraDescriptions,
		Text title,
		ClientIconConfig icon,
                ClientFrameConfig frame,
                float size,
                boolean mergeDescription,
                int cost,
		int requiredSkills,
                int requiredPoints,
                int requiredSpentPoints,
                int requiredExclusions,
                boolean hasLevelRewards
) { }
