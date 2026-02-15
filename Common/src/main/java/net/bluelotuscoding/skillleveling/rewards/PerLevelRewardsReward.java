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
import net.puffish.skillsmod.util.DisposeContext;
import net.puffish.skillsmod.util.LegacyUtils;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.server.network.ServerPlayerEntity;

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
    private final Map<Integer, List<SkillRewardConfig>> onDisableLevelRewards;
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
    private boolean nested = false;

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

    private PerLevelRewardsReward(Map<Integer, List<SkillRewardConfig>> levelRewards,
            Map<Integer, List<SkillRewardConfig>> onDisableLevelRewards, String skillId, int maxLevel,
            int pointsPerLevel, Map<Integer, String> levelDescriptions, Map<Integer, String> levelExtraDescriptions,
            boolean mergeDescription, List<SkillPrerequisite> requiredSkills, boolean allowPartialRewards,
            double scalingFactor) {
        this.levelRewards = levelRewards;
        this.onDisableLevelRewards = onDisableLevelRewards != null ? onDisableLevelRewards : new HashMap<>();
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

    public Map<Integer, List<SkillRewardConfig>> getLevelRewards() {
        return levelRewards;
    }

    public Map<Integer, List<SkillRewardConfig>> getOnDisableLevelRewards() {
        return onDisableLevelRewards;
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

    public void setNested(boolean nested) {
        this.nested = nested;
    }

    public boolean isNested() {
        return nested;
    }

    public void setCachedCategoryId(Identifier cachedCategoryId) {
        this.cachedCategoryId = cachedCategoryId;
    }

    public Identifier getCachedCategoryId() {
        return cachedCategoryId;
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

    public void initializeCount(UUID playerId, int level) {
        counts.put(playerId, level);
        if (level > 0) {
            var activated = activatedLevels.computeIfAbsent(playerId, k -> new java.util.HashSet<>());
            var active = new java.util.HashSet<Integer>();
            for (int i = 1; i <= level; i++) {
                activated.add(i);
                active.add(i);
            }
            lastActiveLevels.put(playerId, active);
        } else {
            lastActiveLevels.remove(playerId);
            activatedLevels.remove(playerId);
        }
    }

    public boolean arePrerequisitesMet(UUID playerId) {
        return arePrerequisitesMet(playerId, null);
    }

    private boolean arePrerequisitesMet(UUID playerId, Identifier categoryId) {
        var mod = SkillLevelingMod.getInstance();
        var manager = mod.getSkillLevelingManager();

        if (manager == null) {
            return true;
        }

        if (skillId != null) {
            var leveledCfg = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
            if (leveledCfg != null && leveledCfg.isLootable) {
                return true;
            }
        }

        if (requiredSkills.isEmpty()) {
            return true;
        }

        if (categoryId == null) {
            if (cachedCategoryId != null) {
                categoryId = cachedCategoryId;
            } else {
                for (var categoryEntry : manager.getPerLevelRewardsRewards().entrySet()) {
                    if (categoryEntry.getValue().containsValue(this)) {
                        cachedCategoryId = categoryEntry.getKey();
                        categoryId = cachedCategoryId;
                        break;
                    }
                }
            }
        }

        if (categoryId == null) {
            mod.getLogger().warn("Could not determine category for skill " + (skillId != null ? skillId : "unknown")
                    + " during prerequisite checking");
            return false;
        }

        return manager.checkSkillPrerequisites(playerId, categoryId, skillId != null ? skillId : "unknown");
    }

    @Override
    public void update(RewardUpdateContext context) {
        var player = context.getPlayer();
        var uuid = player.getUuid();

        int newCount = context.getCount();

        var mod = net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance();
        var manager = mod.getSkillLevelingManager();

        // FALLBACK: If cachedCategoryId is null, try to find this reward instance in
        // the manager's registry.
        if (cachedCategoryId == null && manager != null) {
            for (var entry : manager.getPerLevelRewardsRewards().entrySet()) {
                for (var skillEntry : entry.getValue().entrySet()) {
                    if (skillEntry.getValue() == this) {
                        this.cachedCategoryId = entry.getKey();
                        break;
                    }
                }
                if (cachedCategoryId != null)
                    break;
            }
        }

        if (manager != null && cachedCategoryId != null && skillId != null) {
            if (context.getCount() == 0) {
                newCount = 0;
            } else {
                newCount = manager.getTotalSkillLevel((ServerPlayerEntity) player, cachedCategoryId, skillId);
            }
        }

        boolean prerequisitesMet = arePrerequisitesMet(uuid, cachedCategoryId);
        int effectiveNewCount = (prerequisitesMet || allowPartialRewards) ? newCount : 0;
        counts.put(uuid, effectiveNewCount);

        var previouslyActive = lastActiveLevels.getOrDefault(uuid, java.util.Collections.emptySet());
        var currentlyActive = new java.util.HashSet<Integer>();

        for (var entry : levelRewards.entrySet()) {
            int level = entry.getKey();

            boolean levelPrereqMet = true;
            if (manager != null && cachedCategoryId != null) {
                levelPrereqMet = manager.checkLevelPrerequisites(uuid, cachedCategoryId, skillId, level, false);
            }

            boolean isNowActive = (effectiveNewCount >= level && levelPrereqMet);
            boolean wasPreviouslyActive = previouslyActive.contains(level);

            if (isNowActive)
                currentlyActive.add(level);

            boolean stateChanged = (isNowActive != wasPreviouslyActive);
            boolean isAction = context.isAction() && stateChanged;

            if (!isNowActive && wasPreviouslyActive && stateChanged) {
                var disableList = onDisableLevelRewards.get(level);
                if (disableList != null) {
                    for (var reward : disableList) {
                        try {
                            reward.instance().update(new net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl(
                                    player, 1, true));
                        } catch (Exception e) {
                            mod.getLogger()
                                    .error("Failed to fire disable reward for level " + level + ": " + e.getMessage());
                        }
                    }
                }
            }

            for (var reward : entry.getValue()) {
                try {
                    boolean isAttribute = reward.type().toString().contains("attribute");
                    int innerCount = isNowActive ? 1 : 0;
                    boolean effectiveAction;

                    if (isAttribute) {
                        if (!isNowActive) {
                            boolean hasData = manager != null && cachedCategoryId != null
                                    && manager.hasSkillData(player, cachedCategoryId, skillId);

                            if (!hasData && manager != null) {
                                var activated = activatedLevels.get(uuid);
                                if (activated != null && activated.contains(level)) {
                                    effectiveAction = true;
                                } else {
                                    effectiveAction = false;
                                }
                            } else {
                                effectiveAction = false;
                            }
                        } else {
                            effectiveAction = true;
                        }
                    } else {
                        if (isNowActive && stateChanged && isAction) {
                            var activated = activatedLevels.computeIfAbsent(uuid, k -> new java.util.HashSet<>());
                            if (!activated.contains(level)) {
                                activated.add(level);
                                effectiveAction = true;
                            } else {
                                effectiveAction = false;
                            }
                        } else {
                            if (!isNowActive && stateChanged) {
                                var activated = activatedLevels.get(uuid);
                                if (activated != null) {
                                    activated.remove(level);
                                }
                            }
                            effectiveAction = false;
                        }
                    }

                    reward.instance().update(new net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl(player,
                            innerCount, effectiveAction));
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
        onDisableLevelRewards.values().forEach(list -> list.forEach(cfg -> cfg.dispose(disposeContext)));
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
        var levelRewards = parseLevelRewards(rootObject, "levels", context, problems);
        var onDisableLevelRewards = parseLevelRewards(rootObject, "on_disable_levels", context, problems);

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
                        // Support both skill_id and skill, level and min_level
                        var sId = obj.getString("skill_id").getSuccess()
                                .or(() -> obj.getString("skill").getSuccess());
                        var lvl = obj.getInt("level").getSuccess()
                                .or(() -> obj.getInt("min_level").getSuccess());
                        var cId = obj.get("category_id").getSuccess()
                                .or(() -> obj.get("category").getSuccess())
                                .flatMap(el -> el.getAsString().getSuccess());

                        if (sId.isPresent() && lvl.isPresent()) {
                            return Result.success(new SkillPrerequisite(sId.get(), lvl.get(),
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

        // Explicitly consume fields injected via Mixin or for legacy support
        // to satisfy LegacyUtils.wrapNoUnused validation.
        rootObject.get("required_skill_for_level").getSuccess();

        if (problems.isEmpty()) {
            return Result
                    .success(new PerLevelRewardsReward(levelRewards, onDisableLevelRewards, optSkillId.orElse(null),
                            optMaxLevelTmp.orElse(Math.max(1, definedMaxLevel)),
                            optPointsPerLevelTmp.orElse(0), levelDescriptions, levelExtraDescriptions, mergeDescription,
                            requiredSkills, allowPartialRewards, scalingFactor));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    private static Map<Integer, List<SkillRewardConfig>> parseLevelRewards(JsonObject rootObject, String key,
            ConfigContext context, List<Problem> problems) {
        var rewards = new HashMap<Integer, List<SkillRewardConfig>>();
        var path = rootObject.getPath().getObject(key);

        rootObject.getObject(key).ifSuccess(obj -> {
            obj.stream().forEach(entry -> {
                try {
                    int level = Integer.parseInt(entry.getKey());
                    var list = new ArrayList<SkillRewardConfig>();

                    entry.getValue().getAsArray().ifSuccess(arr -> {
                        arr.getAsList((i, elem) -> {
                            var rewardContext = wrapRewardContext(context, elem);
                            return SkillRewardConfig.parse(elem, rewardContext)
                                    .ifSuccess(cfg -> injectStableUUID(cfg.instance(), elem));
                        })
                                .mapFailure(f -> Problem.combine((java.util.List<Problem>) f))
                                .ifSuccess(list::addAll)
                                .ifFailure(problems::add);
                    }).ifFailure(problems::add);

                    rewards.put(level, list);
                } catch (NumberFormatException e) {
                    problems.add(path.getObject(entry.getKey()).createProblem("Expected integer level"));
                }
            });
        });
        return rewards;
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
