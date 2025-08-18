package net.bluelotuscoding.skillleveling.rewards;

import net.minecraft.util.Identifier;
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
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PerLevelRewardsReward implements Reward {
    public static final Identifier ID = SkillLevelingMod.createIdentifier("per_level_rewards");

    private final Map<Integer, List<SkillRewardConfig>> levelRewards;
    private final String skillId;
    private final int maxLevel;
    private final int pointsPerLevel;
    private final Map<Integer, String> levelDescriptions;
    private final Map<Integer, String> levelExtraDescriptions;
    private final boolean mergeDescription;
    private final Map<UUID, Integer> counts = new HashMap<>();

    private PerLevelRewardsReward(Map<Integer, List<SkillRewardConfig>> levelRewards, String skillId, int maxLevel, int pointsPerLevel, Map<Integer, String> levelDescriptions, Map<Integer, String> levelExtraDescriptions, boolean mergeDescription) {
        this.levelRewards = levelRewards;
        this.skillId = skillId;
        this.maxLevel = maxLevel;
        this.pointsPerLevel = pointsPerLevel;
        this.levelDescriptions = levelDescriptions;
        this.levelExtraDescriptions = levelExtraDescriptions;
        this.mergeDescription = mergeDescription;
    }    public String getSkillId() {
        return skillId;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getPointsPerLevel() {
        return pointsPerLevel;
    }
    
    public Map<Integer, String> getLevelDescriptions() {
        return levelDescriptions;
    }
    
    public Map<Integer, String> getLevelExtraDescriptions() {
        return levelExtraDescriptions;
    }
    
    public boolean isMergeDescription() {
        return mergeDescription;
    }
    
    /**
     * Get the description for a specific level, optionally merging with previous levels
     */
    public String getDescriptionForLevel(int level) {
        if (!mergeDescription || level <= 1) {
            return levelDescriptions.getOrDefault(level, "");
        }
        
        // Merge descriptions from level 1 up to the current level
        StringBuilder merged = new StringBuilder();
        for (int i = 1; i <= level; i++) {
            String desc = levelDescriptions.get(i);
            if (desc != null && !desc.isEmpty()) {
                if (merged.length() > 0) {
                    merged.append("\n");
                }
                merged.append(desc);
            }
        }
        return merged.toString();
    }
    
    /**
     * Get the extra description for a specific level, optionally merging with previous levels
     */
    public String getExtraDescriptionForLevel(int level) {
        if (!mergeDescription || level <= 1) {
            return levelExtraDescriptions.getOrDefault(level, "");
        }
        
        // Merge extra descriptions from level 1 up to the current level
        StringBuilder merged = new StringBuilder();
        for (int i = 1; i <= level; i++) {
            String desc = levelExtraDescriptions.get(i);
            if (desc != null && !desc.isEmpty()) {
                if (merged.length() > 0) {
                    merged.append("\n");
                }
                merged.append(desc);
            }
        }
        return merged.toString();
    }

    public static void register() {
        SkillsAPI.registerReward(ID, PerLevelRewardsReward::parse);
    }

    static Result<PerLevelRewardsReward, Problem> parse(RewardConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(LegacyUtils.wrapNoUnused(obj -> parse(obj, context), context));
    }

    static Result<PerLevelRewardsReward, Problem> parse(JsonObject rootObject, ConfigContext context) {
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
        int definedMaxLevel = levelRewards.keySet().stream().max(Integer::compareTo).orElse(0);

        // Access optional fields and validate values
        var optSkillId = rootObject.getString("skill_id")
                .ifFailure(problems::add)
                .getSuccess();

        var optMaxLevelTmp = rootObject.get("max_skill_level")
                .getSuccess()
                .flatMap(element -> element.getAsInt()
                        .ifFailure(problems::add)
                        .getSuccess());
        optMaxLevelTmp.ifPresent(maxLevel -> {
            if (maxLevel < 1) {
                problems.add(rootObject.getPath().getObject("max_skill_level")
                        .createProblem("Expected a value ≥ 1"));
            }
        });

        var optPointsPerLevelTmp = rootObject.get("points_per_level")
                .getSuccess() // optional
                .flatMap(element -> element.getAsInt()
                                .ifFailure(problems::add)
                                .getSuccess());
        optPointsPerLevelTmp.ifPresent(points -> {
            if (points < 0) {
                problems.add(rootObject.getPath().getObject("points_per_level")
                                .createProblem("Expected a value ≥ 0"));
            }
        });
        int optPointsPerLevel = optPointsPerLevelTmp.orElse(0);

        // Parse descriptions (optional)
        var levelDescriptions = new HashMap<Integer, String>();
        var optDescriptions = rootObject.get("descriptions")
                .getSuccess()
                .flatMap(element -> element.getAsObject()
                        .ifFailure(problems::add)
                        .getSuccess());
        optDescriptions.ifPresent(descObj -> {
            descObj.stream().forEach(entry -> {
                try {
                    var level = Integer.parseInt(entry.getKey());
                    var desc = entry.getValue().getAsString()
                            .ifFailure(problems::add)
                            .getSuccess();
                    desc.ifPresent(d -> levelDescriptions.put(level, d));
                } catch (NumberFormatException e) {
                    problems.add(rootObject.getPath().getObject("descriptions").getObject(entry.getKey())
                            .createProblem("Expected an integer"));
                }
            });
        });

        // Parse extra descriptions (optional)
        var levelExtraDescriptions = new HashMap<Integer, String>();
        var optExtraDescriptions = rootObject.get("extra_descriptions")
                .getSuccess()
                .flatMap(element -> element.getAsObject()
                        .ifFailure(problems::add)
                        .getSuccess());
        optExtraDescriptions.ifPresent(descObj -> {
            descObj.stream().forEach(entry -> {
                try {
                    var level = Integer.parseInt(entry.getKey());
                    var desc = entry.getValue().getAsString()
                            .ifFailure(problems::add)
                            .getSuccess();
                    desc.ifPresent(d -> levelExtraDescriptions.put(level, d));
                } catch (NumberFormatException e) {
                    problems.add(rootObject.getPath().getObject("extra_descriptions").getObject(entry.getKey())
                            .createProblem("Expected an integer"));
                }
            });
        });

        // Parse merge_description (optional, defaults to false)
        var mergeDescription = rootObject.get("merge_description")
                .getSuccess()
                .flatMap(element -> element.getAsBoolean()
                        .ifFailure(problems::add)
                        .getSuccess())
                .orElse(false);

        if (problems.isEmpty()) {
            return Result.success(new PerLevelRewardsReward(levelRewards,
                            optSkillId.orElse(null),
                            optMaxLevelTmp.orElse(Math.max(1, definedMaxLevel)),
                            optPointsPerLevel,
                            levelDescriptions,
                            levelExtraDescriptions,
                            mergeDescription));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    @Override
    public void update(RewardUpdateContext context) {
        var player = context.getPlayer();
        var uuid = player.getUuid();
        int newCount = context.getCount();
        int oldCount = counts.getOrDefault(uuid, 0);
        counts.put(uuid, newCount);

        for (var entry : levelRewards.entrySet()) {
            int level = entry.getKey();
            int count = newCount >= level ? 1 : 0;
            boolean action = level > oldCount && level <= newCount;

            for (var reward : entry.getValue()) {
                reward.instance().update(new RewardUpdateContextImpl(player, count, action));
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
        counts.clear();
    }
}
