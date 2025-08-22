package net.puffish.skillsmod.api.calculation.operation;

import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

public interface OperationConfigContext extends ConfigContext {
	Result<JsonElement, Problem> getData();
}
