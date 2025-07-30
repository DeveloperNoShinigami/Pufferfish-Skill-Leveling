package net.puffish.skill_leveling.impl.rewards;

import net.minecraft.server.MinecraftServer;
import net.puffish.skill_leveling.api.config.ConfigContext;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.reward.RewardConfigContext;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;
import net.puffish.skill_leveling.util.VersionContext;

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
