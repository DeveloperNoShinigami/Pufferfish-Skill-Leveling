package net.puffish.skill_leveling.api.experience.source;

import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;

public interface ExperienceSourceFactory {
	Result<? extends ExperienceSource, Problem> create(ExperienceSourceConfigContext context);
}
