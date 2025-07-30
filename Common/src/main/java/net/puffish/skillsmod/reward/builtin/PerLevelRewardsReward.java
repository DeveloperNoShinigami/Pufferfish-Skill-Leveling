package net.puffish.skillsmod.reward.builtin;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.config.skill.SkillRewardConfig;
import net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl;
import net.puffish.skillsmod.util.DisposeContext;
import net.puffish.skillsmod.util.LegacyUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerLevelRewardsReward implements Reward {
	public static final Identifier ID = SkillsMod.createIdentifier("per_level_rewards");

	private final Map<Integer, List<SkillRewardConfig>> levelRewards;

	private PerLevelRewardsReward(Map<Integer, List<SkillRewardConfig>> levelRewards) {
	this.levelRewards = levelRewards;
	}

	public static void register() {
	SkillsAPI.registerReward(ID, PerLevelRewardsReward::parse);
	}

	private static Result<PerLevelRewardsReward, Problem> parse(RewardConfigContext context) {
	return context.getData()
		.andThen(JsonElement::getAsObject)
		.andThen(LegacyUtils.wrapNoUnused(obj -> parse(obj, context), context));
	}

	private static Result<PerLevelRewardsReward, Problem> parse(JsonObject rootObject, ConfigContext context) {
	var problems = new ArrayList<Problem>();

	var optLevelsMap = rootObject.getObject("levels")
		.andThen(obj -> obj.getAsMap((key, element) ->
			element.getAsArray()
				.andThen(arr -> arr.getAsList((i, e) -> SkillRewardConfig.parse(e, context))
					.mapFailure(Problem::combine))
		).mapFailure(map -> Problem.combine(map.values())))
		.ifFailure(problems::add)
		.getSuccess();

	var levelsPath = rootObject.getPath().getObject("levels");
	var levelRewards = new HashMap<Integer, List<SkillRewardConfig>>();
	optLevelsMap.ifPresent(map -> {
		for (var entry : map.entrySet()) {
		try {
			var level = Integer.parseInt(entry.getKey());
			levelRewards.put(level, entry.getValue());
		} catch (NumberFormatException e) {
			problems.add(levelsPath.getObject(entry.getKey()).createProblem("Expected an integer"));
		}
		}
	});

	// Access optional fields to avoid unused warnings
	rootObject.get("skill_id");
	rootObject.get("max_level");
	rootObject.get("points_per_level");

	if (problems.isEmpty()) {
		return Result.success(new PerLevelRewardsReward(levelRewards));
	} else {
		return Result.failure(Problem.combine(problems));
	}
	}

	@Override
	public void update(RewardUpdateContext context) {
	for (var entry : levelRewards.entrySet()) {
		int level = entry.getKey();
		int count = context.getCount() >= level ? 1 : 0;
		for (var reward : entry.getValue()) {
		reward.instance().update(new RewardUpdateContextImpl(context.getPlayer(), count, context.isAction()));
		}
	}
	}

	@Override
	public void dispose(RewardDisposeContext context) {
	var disposeContext = new DisposeContext(context.getServer());
	for (var rewardList : levelRewards.values()) {
		for (var reward : rewardList) {
		reward.dispose(disposeContext);
		}
	}
	}
}
