package net.bluelotuscoding.puffishskillleveling.api.experience.source;

import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;

public interface ExperienceSourceFactory {
	Result<? extends ExperienceSource, Problem> create(ExperienceSourceConfigContext context);
}
