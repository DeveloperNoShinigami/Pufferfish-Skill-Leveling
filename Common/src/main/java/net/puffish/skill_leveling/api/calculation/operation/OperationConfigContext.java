package net.puffish.skill_leveling.api.calculation.operation;

import net.puffish.skill_leveling.api.config.ConfigContext;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;

public interface OperationConfigContext extends ConfigContext {
	Result<JsonElement, Problem> getData();
}
