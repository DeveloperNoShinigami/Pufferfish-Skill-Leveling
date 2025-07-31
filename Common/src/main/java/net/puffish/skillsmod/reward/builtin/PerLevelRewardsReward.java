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
    private final String skillId;
    private final int maxLevel;
    private final int pointsPerLevel;

    private PerLevelRewardsReward(Map<Integer, List<SkillRewardConfig>> levelRewards, String skillId, int maxLevel, int pointsPerLevel) {
        this.levelRewards = levelRewards;
        this.skillId = skillId;
        this.maxLevel = maxLevel;
        this.pointsPerLevel = pointsPerLevel;
    }

    public String getSkillId() {
        return skillId;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getPointsPerLevel() {
        return pointsPerLevel;
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

        // Access optional fields and validate values
        var optSkillId = rootObject.getString("skill_id")
                .ifFailure(problems::add)
                .getSuccess();

        var optMaxLevelTmp = rootObject.get("max_level")
                .getSuccess() // optional
                .flatMap(element -> element.getAsInt()
                                .ifFailure(problems::add)
                                .getSuccess());
        optMaxLevelTmp.ifPresent(maxLevel -> {
            if (maxLevel < 1) {
                problems.add(rootObject.getPath().getObject("max_level")
                                .createProblem("Expected a value \u2265 1"));
            }
        });
        int optMaxLevel = optMaxLevelTmp.orElse(Integer.MAX_VALUE);

        var optPointsPerLevelTmp = rootObject.get("points_per_level")
                .getSuccess() // optional
                .flatMap(element -> element.getAsInt()
                                .ifFailure(problems::add)
                                .getSuccess());
        optPointsPerLevelTmp.ifPresent(points -> {
            if (points < 0) {
                problems.add(rootObject.getPath().getObject("points_per_level")
                                .createProblem("Expected a value \u2265 0"));
            }
        });
        int optPointsPerLevel = optPointsPerLevelTmp.orElse(0);

        if (problems.isEmpty()) {
            return Result.success(new PerLevelRewardsReward(levelRewards,
                            optSkillId.orElse(null),
                            optMaxLevel,
                            optPointsPerLevel));
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
