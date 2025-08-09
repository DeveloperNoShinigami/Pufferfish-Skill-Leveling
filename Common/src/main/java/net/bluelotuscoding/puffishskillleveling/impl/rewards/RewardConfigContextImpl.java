package net.bluelotuscoding.puffishskillleveling.impl.rewards;

import net.minecraft.server.MinecraftServer;
import net.bluelotuscoding.puffishskillleveling.api.config.ConfigContext;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonElement;
import net.bluelotuscoding.puffishskillleveling.api.reward.RewardConfigContext;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;
import net.bluelotuscoding.puffishskillleveling.util.VersionContext;

public record RewardConfigContextImpl(
		ConfigContext context,
		Result<JsonElement, Problem> maybeDataElement
) implements RewardConfigContext, VersionContext {

	@Override
	public MinecraftServer getServer() {
		return context.getServer();
	}

	@Override
	public void emitWarning(String message) {
		context.emitWarning(message);
	}

	@Override
	public Result<JsonElement, Problem> getData() {
		return maybeDataElement;
	}

	@Override
	public int getVersion() {
		if (context instanceof VersionContext versionContext) {
			return versionContext.getVersion();
		}
		return Integer.MIN_VALUE;
	}
}
