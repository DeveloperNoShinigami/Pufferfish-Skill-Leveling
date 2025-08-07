package net.bluelotuscoding.puffishskillleveling.api.experience.source;

import net.bluelotuscoding.puffishskillleveling.api.config.ConfigContext;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonElement;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;

public interface ExperienceSourceConfigContext extends ConfigContext {
	Result<JsonElement, Problem> getData();
}
