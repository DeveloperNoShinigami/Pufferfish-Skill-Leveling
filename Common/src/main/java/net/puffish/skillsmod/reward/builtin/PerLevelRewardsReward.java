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
import net.puffish.skillsmod.impl.rewards.SkillRewardUpdateContext;
import net.puffish.skillsmod.impl.rewards.SkillRewardUpdateContextImpl;
import org.apache.commons.lang3.RandomStringUtils;
import net.puffish.skillsmod.util.DisposeContext;
import net.puffish.skillsmod.util.LegacyUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerLevelRewardsReward implements Reward {
        public static final Identifier ID = SkillsMod.createIdentifier("per_level_rewards");
    private static final String PREFIX = "per_level_rewards.";

    private final Map<Integer, List<SkillRewardConfig>> levelRewards;
    private final String skillId;
    private final int maxLevel;
    private final int pointsPerLevel;
    private final Identifier source;

    private PerLevelRewardsReward(Map<Integer, List<SkillRewardConfig>> levelRewards, String skillId, int maxLevel, int pointsPerLevel, Identifier source) {
        this.levelRewards = levelRewards;
        this.skillId = skillId;
        this.maxLevel = maxLevel;
        this.pointsPerLevel = pointsPerLevel;
        this.source = source;
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

        var optSkillId = rootObject.getString("skill_id")
                .ifFailure(problems::add)
                .getSuccess();

        var optMaxLevel = rootObject.getInt("max_level")
                .ifFailure(problems::add)
                .getSuccess();

        var optPointsPerLevel = rootObject.getInt("points_per_level")
                .ifFailure(problems::add)
                .getSuccess();

        if (problems.isEmpty()) {
                return Result.success(new PerLevelRewardsReward(
                                levelRewards,
                                optSkillId.orElse(null),
                                optMaxLevel.orElse(Integer.MAX_VALUE),
                                optPointsPerLevel.orElse(0),
                                SkillsMod.createIdentifier(PREFIX + org.apache.commons.lang3.RandomStringUtils.random(16, "abcdefghijklmnopqrstuvwxyz0123456789"))
                ));
        } else {
                return Result.failure(Problem.combine(problems));
        }
	}

        @Override
        public void update(RewardUpdateContext context) {
                if (skillId != null) {
                        if (!(context instanceof SkillRewardUpdateContext sruc) || !skillId.equals(sruc.getSkillId())) {
                                return;
                        }
                }

                int currentLevel = Math.min(context.getCount(), maxLevel);

                Identifier categoryId = null;
                String currentSkillId = null;
                if (context instanceof SkillRewardUpdateContext sruc) {
                        categoryId = sruc.getCategoryId();
                        currentSkillId = sruc.getSkillId();
                }

                for (var entry : levelRewards.entrySet()) {
                        int level = entry.getKey();
                        int count = currentLevel >= level ? 1 : 0;
                        for (var reward : entry.getValue()) {
                                reward.instance().update(new SkillRewardUpdateContextImpl(
                                                context.getPlayer(),
                                                count,
                                                context.isAction(),
                                                categoryId,
                                                currentSkillId
                                ));
                        }
                }

                if (pointsPerLevel != 0 && categoryId != null) {
                        SkillsMod.getInstance().setPoints(
                                        context.getPlayer(),
                                        categoryId,
                                        source,
                                        pointsPerLevel * currentLevel,
                                        !context.isAction()
                        );
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

        context.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                SkillsAPI.streamCategories().forEach(category -> {
                        category.setPoints(player, source, 0);
                });
        });
        }
}
