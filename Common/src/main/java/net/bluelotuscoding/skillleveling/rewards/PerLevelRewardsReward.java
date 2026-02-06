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
import java.util.stream.Collectors;

/**
 * Enhanced PerLevelRewardsReward with skill prerequisites and advanced features
 */
public class PerLevelRewardsReward implements Reward {
    public static final Identifier ID = SkillLevelingMod.createIdentifier("per_level_rewards");

    private final Map<Integer, List<SkillRewardConfig>> levelRewards;
    private final String skillId;
    private final int maxLevel;
    private final int pointsPerLevel;
    private final Map<Integer, String> levelDescriptions;
    private final Map<Integer, String> levelExtraDescriptions;
    private final boolean mergeDescription;
    private final List<SkillPrerequisite> requiredSkills;
    private final boolean allowPartialRewards;
    private final double scalingFactor;
    private final Map<UUID, Integer> counts = new HashMap<>();
    private final Map<UUID, java.util.Set<Integer>> activatedLevels = new HashMap<>();
    private final Map<UUID, java.util.Set<Integer>> lastActiveLevels = new HashMap<>();
    private Identifier cachedCategoryId = null;

    public static class SkillPrerequisite {
        private final String skillId;
        private final int level;
        private final String categoryId;

        public SkillPrerequisite(String skillId, int level, String categoryId) {
            this.skillId = skillId;
            this.level = level;
            this.categoryId = categoryId;
        }

        public String getSkillId() {
            return skillId;
        }

        public int getLevel() {
            return level;
        }

        public String getCategoryId() {
            return categoryId;
        }

        @Override
        public String toString() {
            return String.format("%s:%d%s", skillId, level, categoryId != null ? "@" + categoryId : "");
        }
    }

    private PerLevelRewardsReward(Map<Integer, List<SkillRewardConfig>> levelRewards, String skillId, int maxLevel,
            int pointsPerLevel, Map<Integer, String> levelDescriptions, Map<Integer, String> levelExtraDescriptions,
            boolean mergeDescription, List<SkillPrerequisite> requiredSkills, boolean allowPartialRewards,
            double scalingFactor) {
        this.levelRewards = levelRewards;
        this.skillId = skillId;
        this.maxLevel = maxLevel;
        this.pointsPerLevel = pointsPerLevel;
        this.levelDescriptions = levelDescriptions;
        this.levelExtraDescriptions = levelExtraDescriptions;
        this.mergeDescription = mergeDescription;
        this.requiredSkills = requiredSkills != null ? requiredSkills : new ArrayList<>();
        this.allowPartialRewards = allowPartialRewards;
        this.scalingFactor = scalingFactor;
    }

    public String getSkillId() {
        return skillId;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getPointsPerLevel() {
        if (pointsPerLevel > 0)
            return pointsPerLevel;
        if (skillId != null) {
            var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
            if (config != null)
                return config.pointsPerLevel;
        }
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

    public List<SkillPrerequisite> getRequiredSkills() {
        return requiredSkills;
    }

    public int getEffectivePointsPerLevel(int currentLevel) {
        int baseCost = getPointsPerLevel();
        if (scalingFactor == 1.0) {
            return baseCost;
        }
        return (int) Math.ceil(baseCost * Math.pow(scalingFactor, currentLevel - 1));
    }

    public String getDescriptionForLevel(int level) {
        if (!mergeDescription || level <= 1) {
            String desc = levelDescriptions.getOrDefault(level, "");
            if (level == 1 && !requiredSkills.isEmpty()) {
                String prereqStr = "Requires: " + getRequiredSkills().stream()
                        .map(p -> p.toString()).collect(Collectors.joining(", "));
                desc = desc.isEmpty() ? prereqStr : desc + "\n" + prereqStr;
            }
            return desc;
        }

        StringBuilder merged = new StringBuilder();
        for (int i = 1; i <= level; i++) {
            String desc = levelDescriptions.get(i);
            if (desc != null && !desc.isEmpty()) {
                if (merged.length() > 0)
                    merged.append("\n• ");
                else
                    merged.append("• ");
                merged.append(desc);
            }
        }
        if (!requiredSkills.isEmpty()) {
            merged.append("\nRequires: ").append(getRequiredSkills().stream()
                    .map(p -> p.toString()).collect(Collectors.joining(", ")));
        }
        return merged.toString();
    }

    public String getExtraDescriptionForLevel(int level) {
        if (!mergeDescription || level <= 1) {
            return levelExtraDescriptions.getOrDefault(level, "");
        }

        StringBuilder merged = new StringBuilder();
        for (int i = 1; i <= level; i++) {
            String desc = levelExtraDescriptions.get(i);
            if (desc != null && !desc.isEmpty()) {
                if (merged.length() > 0)
                    merged.append("\n→ ");
                else
                    merged.append("→ ");
                merged.append(desc);
            }
        }
        return merged.toString();
    }

    /**
     * Initialize the internal trackers with the current player level.
     * PROTECTS against multiple calls in the same session to prevent join-time
     * re-triggering.
     */
    public void initializeCount(UUID playerId, Identifier categoryId, int level) {
        // Only load from NBT if we haven't already initialized this player in this
        // session.
        // This prevents Pufferfish from overwriting our "just activated" rewards during
        // the burst of refresh calls on join.
        if (counts.containsKey(playerId)) {
            counts.put(playerId, level);
            return;
        }

        counts.put(playerId, level);
        this.cachedCategoryId = categoryId;

        if (level > 0) {
            var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
            if (manager != null) {
                var activated = manager.getDataManager().getActivatedLevels(playerId, categoryId, skillId);
                activatedLevels.put(playerId, activated);
            }

            var active = new java.util.HashSet<Integer>();
            for (int i = 1; i <= level; i++) {
                active.add(i);
            }
            lastActiveLevels.put(playerId, active);
        } else {
            lastActiveLevels.remove(playerId);
        }
    }

    @Override
    public void update(RewardUpdateContext context) {
        var player = context.getPlayer();
        var uuid = player.getUuid();

        Integer oldCount = counts.get(uuid);
        int effectiveOldCount = (oldCount != null) ? oldCount : 0;
        int effectiveNewCount = context.getCount();
        counts.put(uuid, effectiveNewCount);

        var currentlyActive = new java.util.HashSet<Integer>();

        SkillLevelingMod.getInstance().getLogger()
                .debug("[REWARD DEBUG] Updating skill " + skillId + " for " + player.getName().getString()
                        + ": old=" + effectiveOldCount + ", new=" + effectiveNewCount + ", action="
                        + context.isAction());

        var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();

        for (var entry : levelRewards.entrySet()) {
            int level = entry.getKey();

            boolean levelPrereqMet = true;
            if (manager != null && cachedCategoryId != null) {
                levelPrereqMet = manager.checkLevelPrerequisites(uuid, cachedCategoryId, skillId, level, false);
            }

            boolean isNowActive = (effectiveNewCount >= level && levelPrereqMet);
            if (isNowActive)
                currentlyActive.add(level);

            for (var reward : entry.getValue()) {
                try {
                    String type = reward.type().toString();
                    boolean isAttribute = type.equals("puffish_skills:attribute");

                    int count = isNowActive ? 1 : 0;
                    boolean effectiveAction;

                    if (isAttribute) {
                        effectiveAction = isNowActive;
                    } else {
                        var activated = activatedLevels.computeIfAbsent(uuid, k -> new java.util.HashSet<>());
                        if (isNowActive && !activated.contains(level)) {
                            activated.add(level);
                            effectiveAction = true;
                            if (manager != null && cachedCategoryId != null) {
                                manager.getDataManager().setActivatedLevel(player, cachedCategoryId, skillId, level);
                            }
                        } else {
                            effectiveAction = false;
                        }
                    }

                    reward.instance().update(new RewardUpdateContextImpl(player, count, effectiveAction));
                } catch (Exception e) {
                    SkillLevelingMod.getInstance().getLogger()
                            .error("Failed to apply reward for level " + level + ": " + e.getMessage());
                }
            }
        }
        lastActiveLevels.put(uuid, currentlyActive);
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        var disposeContext = new DisposeContext(context.getServer());
        levelRewards.values().forEach(list -> list.forEach(cfg -> cfg.dispose(disposeContext)));
        counts.clear();
        activatedLevels.clear();
        lastActiveLevels.clear();
    }

    public static void register() {
        SkillsAPI.registerReward(ID, PerLevelRewardsReward::parse);
    }

    static Result<PerLevelRewardsReward, Problem> parse(RewardConfigContext context) {
        return context.getData().andThen(JsonElement::getAsObject)
                .andThen(LegacyUtils.wrapNoUnused(obj -> parse(obj, context), context));
    }

    static Result<PerLevelRewardsReward, Problem> parse(JsonObject rootObject, ConfigContext context) {
        var problems = new ArrayList<Problem>();

        var optSkillId = rootObject.getString("skill_id").ifFailure(problems::add).getSuccess();
        var optLevelsMap = rootObject.getObject("levels")
                .andThen(obj -> obj.getAsMap((key, element) -> element.getAsArray()
                        .andThen(arr -> arr.getAsList((i, e) -> SkillRewardConfig.parse(e, context))
                                .mapFailure(Problem::combine)))
                        .mapFailure(map -> Problem.combine(map.values())))
                .ifFailure(problems::add).getSuccess();

        var levelsPath = rootObject.getPath().getObject("levels");
        var levelRewards = new HashMap<Integer, List<SkillRewardConfig>>();
        optLevelsMap.ifPresent(map -> {
            for (var entry : map.entrySet()) {
                try {
                    var level = Integer.parseInt(entry.getKey());
                    var list = new ArrayList<SkillRewardConfig>();
                    rootObject.getObject("levels").andThen(obj -> obj.getArray(entry.getKey())).ifSuccess(arr -> {
                        arr.getAsList((i, elem) -> {
                            var rewardContext = wrapRewardContext(context, elem);
                            var res = SkillRewardConfig.parse(elem, rewardContext);
                            res.ifSuccess(cfg -> {
                                injectStableUUID(cfg.instance(), elem);
                                list.add(cfg);
                            });
                            res.ifFailure(problems::add);
                            return res;
                        });
                    });
                    levelRewards.put(level, list);
                } catch (NumberFormatException e) {
                    problems.add(levelsPath.getObject(entry.getKey()).createProblem("Expected integer level"));
                }
            }
        });

        int definedMaxLevel = levelRewards.keySet().stream().max(Integer::compareTo).orElse(0);
        var optMaxLevelTmp = rootObject.get("max_skill_level").getSuccess()
                .flatMap(e -> e.getAsInt().ifFailure(problems::add).getSuccess());
        optMaxLevelTmp.ifPresent(maxLevel -> {
            if (maxLevel < 1)
                problems.add(rootObject.getPath().getObject("max_skill_level").createProblem("Level must be ≥ 1"));
        });

        var optPointsPerLevelTmp = rootObject.get("points_per_level").getSuccess()
                .flatMap(e -> e.getAsInt().ifFailure(problems::add).getSuccess());
        optPointsPerLevelTmp.ifPresent(points -> {
            if (points < 0)
                problems.add(rootObject.getPath().getObject("points_per_level").createProblem("Points must be ≥ 0"));
        });

        var requiredSkills = new ArrayList<SkillPrerequisite>();
        rootObject.get("prerequisite_skills").getSuccess()
                .flatMap(e -> e.getAsArray().ifFailure(problems::add).getSuccess()).ifPresent(arr -> {
                    arr.getAsList((index, element) -> {
                        var objRes = element.getAsObject().ifFailure(problems::add);
                        if (objRes.getFailure().isPresent())
                            return Result.failure(null);
                        var obj = objRes.getSuccess().get();
                        var sId = obj.getString("skill_id").ifFailure(problems::add);
                        var lvl = obj.getInt("level").ifFailure(problems::add);
                        var cId = obj.get("category_id").getSuccess()
                                .flatMap(el -> el.getAsString().ifFailure(problems::add).getSuccess());
                        if (sId.getSuccess().isPresent() && lvl.getSuccess().isPresent()) {
                            return Result.success(new SkillPrerequisite(sId.getSuccess().get(), lvl.getSuccess().get(),
                                    cId.orElse(null)));
                        }
                        return Result.failure(null);
                    }).ifSuccess(list -> list.forEach(p -> {
                        if (p != null)
                            requiredSkills.add(p);
                    }));
                });

        var allowPartialRewards = rootObject.get("allow_partial_rewards").getSuccess()
                .flatMap(e -> e.getAsBoolean().ifFailure(problems::add).getSuccess()).orElse(false);
        var scalingFactor = rootObject.get("scaling_factor").getSuccess()
                .flatMap(e -> e.getAsDouble().ifFailure(problems::add).getSuccess()).orElse(1.0);
        var mergeDescription = rootObject.get("merge_description").getSuccess()
                .flatMap(e -> e.getAsBoolean().ifFailure(problems::add).getSuccess()).orElse(false);

        var levelDescriptions = new HashMap<Integer, String>();
        rootObject.get("descriptions").getSuccess().flatMap(e -> e.getAsObject().ifFailure(problems::add).getSuccess())
                .ifPresent(obj -> obj.stream().forEach(entry -> {
                    try {
                        levelDescriptions.put(Integer.parseInt(entry.getKey()),
                                entry.getValue().getAsString().getSuccess().orElse(""));
                    } catch (Exception e) {
                    }
                }));

        var levelExtraDescriptions = new HashMap<Integer, String>();
        rootObject.get("extra_descriptions").getSuccess()
                .flatMap(e -> e.getAsObject().ifFailure(problems::add).getSuccess())
                .ifPresent(obj -> obj.stream().forEach(entry -> {
                    try {
                        levelExtraDescriptions.put(Integer.parseInt(entry.getKey()),
                                entry.getValue().getAsString().getSuccess().orElse(""));
                    } catch (Exception e) {
                    }
                }));

        if (problems.isEmpty()) {
            return Result.success(new PerLevelRewardsReward(levelRewards, optSkillId.orElse(null),
                    optMaxLevelTmp.orElse(Math.max(1, definedMaxLevel)),
                    optPointsPerLevelTmp.orElse(0), levelDescriptions, levelExtraDescriptions, mergeDescription,
                    requiredSkills, allowPartialRewards, scalingFactor));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    private static ConfigContext wrapRewardContext(ConfigContext context, JsonElement data) {
        if (context instanceof RewardConfigContext rewardContext) {
            return new RewardConfigContext() {
                @Override
                public net.minecraft.server.MinecraftServer getServer() {
                    return rewardContext.getServer();
                }

                @Override
                public void emitWarning(String message) {
                    rewardContext.emitWarning(message);
                }

                @Override
                public Result<JsonElement, Problem> getData() {
                    return Result.success(data);
                }
            };
        }
        return context;
    }

    private static void injectStableUUID(net.puffish.skillsmod.api.reward.Reward reward, JsonElement element) {
        if (reward != null && element != null && reward.getClass().getName().contains("AttributeReward")) {
            try {
                java.lang.reflect.Field uuidsField = reward.getClass().getDeclaredField("uuids");
                uuidsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<java.util.UUID> uuids = (java.util.List<java.util.UUID>) uuidsField.get(reward);
                if (uuids != null) {
                    uuids.clear();
                    String path = element.getPath().toString();
                    for (int i = 0; i < 100; i++) {
                        uuids.add(java.util.UUID.nameUUIDFromBytes((path + "_level_" + i).getBytes()));
                    }
                }
            } catch (Exception e) {
            }
        }
    }
}
