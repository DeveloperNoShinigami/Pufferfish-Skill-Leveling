package net.puffish.skillsmod.api.experience.source;

import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

public interface ExperienceSourceConfigContext extends ConfigContext {
	Result<JsonElement, Problem> getData();
}
