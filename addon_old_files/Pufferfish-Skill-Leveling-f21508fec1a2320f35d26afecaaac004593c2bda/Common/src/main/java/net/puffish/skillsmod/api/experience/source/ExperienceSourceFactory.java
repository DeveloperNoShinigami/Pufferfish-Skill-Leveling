package net.puffish.skillsmod.api.experience.source;

import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

public interface ExperienceSourceFactory {
	Result<? extends ExperienceSource, Problem> create(ExperienceSourceConfigContext context);
}
