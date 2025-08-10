package net.bluelotuscoding.puffishskillleveling.client.config.skill;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.client.config.ClientFrameConfig;
import net.puffish.skillsmod.client.config.ClientIconConfig;

/**
 * Holds extended skill definition information sent by the server. This mirrors
 * {@link net.puffish.skillsmod.client.config.skill.ClientSkillDefinitionConfig}
 * but adds additional fields used by the skill leveling addon.
 */
public record ExtendedClientSkillDefinitionConfig(
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
