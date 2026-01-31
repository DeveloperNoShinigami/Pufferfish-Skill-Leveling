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
 * 
 * New Features:
 * - Skill prerequisites with required_skill field
 * - Enhanced validation and error handling
 * - Better description merging
 * - Improved reward distribution logic
 * - Dynamic level scaling
 * - Conditional reward activation
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

    /**
     * Represents a skill prerequisite
     */
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
            return String.format("%s:%d%s", skillId, level,
                    categoryId != null ? "@" + categoryId : "");
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
        if (pointsPerLevel > 0) {
            return pointsPerLevel;
        }
        // Fallback to global leveled config if available
        if (skillId != null) {
            var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
            if (config != null) {
                return config.pointsPerLevel;
            }
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

    public boolean allowsPartialRewards() {
        return allowPartialRewards;
    }

    public double getScalingFactor() {
        return scalingFactor;
    }

    /**
     * PRE-SEED COUNT: Initialize the player's count without triggering any rewards.
     * This must be called on player join BEFORE any refreshAllRewards call to
     * ensure
     * that oldCount == newCount during refresh, preventing commands from re-firing.
     */
    public void initializeCount(UUID playerId, int level) {
        counts.put(playerId, level);
    }

    /**
     * Check if all skill prerequisites are met for a given player
     */
    public boolean arePrerequisitesMet(UUID playerId) {
        if (requiredSkills.isEmpty()) {
            return true;
        }

        // Use SkillLevelingManager for prerequisite checking
        var mod = net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance();
        if (mod == null) {
            // Addon not initialized yet
            SkillLevelingMod.getInstance().getLogger().warn("Addon not initialized for prerequisite checking");
            return false;
        }

        var manager = mod.getSkillLevelingManager();
        if (manager == null) {
            mod.getLogger().warn("SkillLevelingManager not available for prerequisite checking");
            return false; // Fail safe - don't allow rewards if we can't check
        }

        // For prerequisite checking, we need to know which category this skill belongs
        // to
        // We'll attempt to find it by looking through registered rewards
        Identifier categoryId = null;
        for (var categoryEntry : manager.getPerLevelRewardsRewards().entrySet()) {
            for (var skillEntry : categoryEntry.getValue().entrySet()) {
                if (skillEntry.getValue() == this && skillId != null) {
                    categoryId = categoryEntry.getKey();
                    break;
                }
            }
            if (categoryId != null) {
                break;
            }
        }

        if (categoryId == null) {
            mod.getLogger().warn("Could not determine category for skill " + (skillId != null ? skillId : "unknown")
                    + " during prerequisite checking");
            return false; // Fail safe
        }

        mod.getLogger().debug("Checking prerequisites for skill " + skillId + " in category " + categoryId + ": "
                + requiredSkills.size() + " requirements");

        return manager.checkSkillPrerequisites(playerId, categoryId, skillId != null ? skillId : "unknown");
    }

    /**
     * Get the effective points per level with scaling applied
     */
    public int getEffectivePointsPerLevel(int currentLevel) {
        int baseCost = getPointsPerLevel();
        if (scalingFactor == 1.0) {
            return baseCost;
        }

        return (int) Math.ceil(baseCost * Math.pow(scalingFactor, currentLevel - 1));
    }

    /**
     * Get all prerequisites as a formatted string
     */
    public String getPrerequisitesString() {
        if (requiredSkills.isEmpty()) {
            return "None";
        }

        return requiredSkills.stream()
                .map(SkillPrerequisite::toString)
                .collect(Collectors.joining(", "));
    }

    /**
     * Get the description for a specific level, optionally merging with previous
     * levels
     */
    public String getDescriptionForLevel(int level) {
        if (!mergeDescription || level <= 1) {
            String desc = levelDescriptions.getOrDefault(level, "");

            // Add prerequisite info if this is level 1 and prerequisites exist
            if (level == 1 && !requiredSkills.isEmpty()) {
                String prereqStr = "Requires: " + getPrerequisitesString();
                desc = desc.isEmpty() ? prereqStr : desc + "\n" + prereqStr;
            }

            return desc;
        }

        // Enhanced merging with better formatting
        StringBuilder merged = new StringBuilder();
        for (int i = 1; i <= level; i++) {
            String desc = levelDescriptions.get(i);
            if (desc != null && !desc.isEmpty()) {
                if (merged.length() > 0) {
                    merged.append("\n• ");
                } else {
                    merged.append("• ");
                }
                merged.append(desc);
            }
        }

        // Add prerequisite info
        if (!requiredSkills.isEmpty()) {
            String prereqStr = "\nRequires: " + getPrerequisitesString();
            merged.append(prereqStr);
        }

        return merged.toString();
    }

    /**
     * Get the extra description for a specific level, optionally merging with
     * previous levels
     */
    public String getExtraDescriptionForLevel(int level) {
        if (!mergeDescription || level <= 1) {
            return levelExtraDescriptions.getOrDefault(level, "");
        }

        // Enhanced merging with better formatting
        StringBuilder merged = new StringBuilder();
        for (int i = 1; i <= level; i++) {
            String desc = levelExtraDescriptions.get(i);
            if (desc != null && !desc.isEmpty()) {
                if (merged.length() > 0) {
                    merged.append("\n→ ");
                } else {
                    merged.append("→ ");
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

        // Parse skill_id (optional)
        var optSkillId = rootObject.getString("skill_id")
                .ifFailure(problems::add)
                .getSuccess();

        // Parse levels (required)
        var optLevelsMap = rootObject.getObject("levels")
                .andThen(obj -> obj.getAsMap((key, element) -> element.getAsArray()
                        .andThen(arr -> arr.getAsList((i, e) -> SkillRewardConfig.parse(e, context))
                                .mapFailure(Problem::combine)))
                        .mapFailure(map -> Problem.combine(map.values())))
                .ifFailure(problems::add)
                .getSuccess();

        var levelsPath = rootObject.getPath().getObject("levels");
        var levelRewards = new HashMap<Integer, List<SkillRewardConfig>>();
        optLevelsMap.ifPresent(map -> {
            for (var entry : map.entrySet()) {
                try {
                    var level = Integer.parseInt(entry.getKey());
                    if (level < 1) {
                        problems.add(levelsPath.getObject(entry.getKey())
                                .createProblem("Level must be ≥ 1"));
                    } else {
                        // REWARD IDENTITY FIX: Use wrapped context for each reward
                        // This ensures rewards at different levels get unique identities (paths)
                        // allowing them to stack correctly (e.g. +2 + +2 = +4)
                        var list = new ArrayList<SkillRewardConfig>();
                        var levelArrayResult = rootObject.getObject("levels")
                                .andThen(obj -> obj.getArray(entry.getKey()));

                        levelArrayResult.ifSuccess(levelArray -> {
                            levelArray.getAsList((i, rewardElement) -> {
                                // REWARD IDENTITY: Rely on wrapped context to provide unique paths
                                // for each reward, which Pufferfish uses for identity.
                                var rewardContext = wrapRewardContext(context, rewardElement);
                                var rewardConfigResult = SkillRewardConfig.parse(rewardElement, rewardContext);

                                rewardConfigResult.ifSuccess(config -> {
                                    // STABLE IDENTITY FIX: Inject deterministic UUIDs for AttributeRewards
                                    injectStableUUID(config.instance(), rewardElement);
                                    list.add(config);
                                });

                                rewardConfigResult.ifFailure(problems::add);
                                return rewardConfigResult;
                            });
                        });
                        levelRewards.put(level, list);
                    }
                } catch (NumberFormatException e) {
                    problems.add(levelsPath.getObject(entry.getKey()).createProblem("Expected an integer"));
                }
            }
        });
        int definedMaxLevel = levelRewards.keySet().stream().max(Integer::compareTo).orElse(0);

        // Parse max_skill_level (optional)
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

        // Parse points_per_level (optional)
        var optPointsPerLevelTmp = rootObject.get("points_per_level")
                .getSuccess()
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

        // Parse required_skill field (NEW FEATURE)
        var requiredSkills = new ArrayList<SkillPrerequisite>();
        var optRequiredSkills = rootObject.get("required_skill")
                .getSuccess()
                .flatMap(element -> element.getAsArray()
                        .ifFailure(problems::add)
                        .getSuccess());

        optRequiredSkills.ifPresent(array -> {
            var arrayResult = array.getAsList((index, element) -> {
                var prereqResult = element.getAsObject()
                        .ifFailure(problems::add);

                if (prereqResult.getFailure().isPresent()) {
                    return prereqResult.mapSuccess(obj -> (SkillPrerequisite) null);
                }

                var prereqObj = prereqResult.getSuccess().get();
                var skillIdResult = prereqObj.getString("skill_id")
                        .ifFailure(problems::add);
                var levelResult = prereqObj.getInt("level")
                        .ifFailure(problems::add);
                var categoryIdResult = prereqObj.get("category_id")
                        .getSuccess()
                        .flatMap(elem -> elem.getAsString()
                                .ifFailure(problems::add)
                                .getSuccess());

                if (skillIdResult.getSuccess().isPresent() && levelResult.getSuccess().isPresent()) {
                    var level = levelResult.getSuccess().get();
                    if (level < 1) {
                        problems.add(element.getPath().createProblem("Prerequisite level must be ≥ 1"));
                        return Result.failure(Problem.message("Invalid level"));
                    } else {
                        return Result.success(new SkillPrerequisite(
                                skillIdResult.getSuccess().get(),
                                level,
                                categoryIdResult.orElse(null)));
                    }
                } else {
                    return Result.failure(Problem.message("Missing required fields"));
                }
            });

            arrayResult.ifSuccess(list -> {
                for (var prerequisite : list) {
                    if (prerequisite != null) {
                        requiredSkills.add(prerequisite);
                    }
                }
            });
        });

        // Parse allow_partial_rewards (optional, defaults to false)
        var allowPartialRewards = rootObject.get("allow_partial_rewards")
                .getSuccess()
                .flatMap(element -> element.getAsBoolean()
                        .ifFailure(problems::add)
                        .getSuccess())
                .orElse(false);

        // Parse scaling_factor (optional, defaults to 1.0)
        var scalingFactorOpt = rootObject.get("scaling_factor")
                .getSuccess()
                .flatMap(element -> element.getAsDouble()
                        .ifFailure(problems::add)
                        .getSuccess());
        double scalingFactor = scalingFactorOpt.orElse(1.0);

        if (scalingFactor <= 0) {
            problems.add(rootObject.getPath().getObject("scaling_factor")
                    .createProblem("Scaling factor must be > 0"));
        }

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
                    mergeDescription,
                    requiredSkills,
                    allowPartialRewards,
                    scalingFactor));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    /**
     * Helper to wrap context for sub-reward parsing.
     * This provides each reward with its own data and path identity.
     */
    private static ConfigContext wrapRewardContext(ConfigContext context,
            net.puffish.skillsmod.api.json.JsonElement data) {
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
                public Result<net.puffish.skillsmod.api.json.JsonElement, Problem> getData() {
                    return Result.success(data);
                }
            };
        }
        return context;
    }

    @Override
    public void update(RewardUpdateContext context) {
        var player = context.getPlayer();
        var uuid = player.getUuid();
        int newCount = context.getCount();
        int oldCount = counts.getOrDefault(uuid, 0);
        // IMPORTANT: Don't overwrite counts yet - we need oldCount preserved for
        // reversion logic!

        SkillLevelingMod.getInstance().getLogger()
                .info("[REWARD DEBUG] Updating skill " + skillId + " for " + player.getName().getString()
                        + ": old=" + oldCount + ", new=" + newCount + ", action=" + context.isAction());

        // Check prerequisites before applying any rewards
        boolean prerequisitesMet = arePrerequisitesMet(uuid);

        // Effective count logic: Handle prerequisite loss correctly.
        // If prereqs are NOT met and partial rewards ARE NOT allowed, effective count
        // is 0.
        // This ensures existing rewards are REMOVED (reverted) when prerequisites are
        // lost.
        int effectiveNewCount = (prerequisitesMet || allowPartialRewards) ? newCount : 0;
        // REVERSION FIX: Use the REAL old count, not re-read from already-overwritten
        // map
        int effectiveOldCount = oldCount;
        // NOW we can safely update the counts map
        counts.put(uuid, effectiveNewCount);

        if (!prerequisitesMet) {
            if (!allowPartialRewards) {
                SkillLevelingMod.getInstance().getLogger().info("[REWARD] Prerequisites NOT met for skill "
                        + (skillId != null ? skillId : "unknown") + " - Deactivating all rewards.");
            } else {
                SkillLevelingMod.getInstance().getLogger().warn("[REWARD] Prerequisites NOT met for skill "
                        + (skillId != null ? skillId : "unknown") + " - Continuing due to allow_partial_rewards.");
            }
        }

        // Apply level rewards with enhanced logic
        for (var entry : levelRewards.entrySet()) {
            int level = entry.getKey();
            int levelCount = effectiveNewCount >= level ? 1 : 0;
            int oldLevelCount = effectiveOldCount >= level ? 1 : 0;

            // Persistence Fix: Action is true if THIS specific level's state changed
            // (either added or removed).
            boolean action = context.isAction() && (levelCount != oldLevelCount);

            // Sub-count for the Pufferfish reward (0 or 1)
            int count = levelCount;

            // Apply scaling factor if configured
            if (action && scalingFactor != 1.0) {
                // Modify reward effectiveness based on scaling
                double effectiveMultiplier = Math.pow(scalingFactor, level - 1);
                SkillLevelingMod.getInstance().getLogger().debug("Applying scaling factor " + scalingFactor
                        + " for level " + level + ", effective multiplier: " + effectiveMultiplier);

                // TODO: Apply scaling to rewards if supported by reward types
            }

            for (var reward : entry.getValue()) {
                try {
                    SkillLevelingMod.getInstance().getLogger()
                            .info("[REWARD DEBUG] - Sub-Reward Update: " + (skillId != null ? skillId : "unnamed")
                                    + " Lvl " + level
                                    + " -> count=" + count + ", action=" + action + ", type=" + reward.type());
                    reward.instance().update(new RewardUpdateContextImpl(player, count, action));
                } catch (Exception e) {
                    SkillLevelingMod.getInstance().getLogger().error("Failed to apply reward for skill "
                            + (skillId != null ? skillId : "unknown") + " level " + level + ": " + e.getMessage());

                    if (!allowPartialRewards) {
                        // If partial rewards not allowed, stop processing on error
                        throw new RuntimeException("Reward application failed", e);
                    }
                    // Otherwise continue with next reward
                }
            }
        }
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        var disposeContext = new DisposeContext(context.getServer());
        levelRewards.values().forEach(rewardList -> {
            rewardList.forEach(rewardConfig -> {
                rewardConfig.dispose(disposeContext);
            });
        });
        counts.clear();
    }

    /**
     * STABLE IDENTITY FIX: Injects deterministic UUIDs into Pufferfish
     * AttributeRewards.
     * 
     * Pufferfish's built-in AttributeReward generates random UUIDs on its first
     * update.
     * This makes it impossible to remove/revert attributes if the reward object is
     * re-created
     * (e.g., during config reloads or across server restarts).
     * 
     * We solve this by using the JSON path of the reward as a stable seed for a
     * deterministic UUID.
     */
    private static void injectStableUUID(net.puffish.skillsmod.api.reward.Reward reward, JsonElement element) {
        if (reward == null || element == null)
            return;

        // Target Specifically AttributeReward for stable UUIDs.
        if (reward.getClass().getName().equals("net.puffish.skillsmod.reward.builtin.AttributeReward")) {
            try {
                java.lang.reflect.Field uuidsField = reward.getClass().getDeclaredField("uuids");
                uuidsField.setAccessible(true);

                @SuppressWarnings("unchecked")
                java.util.List<java.util.UUID> uuids = (java.util.List<java.util.UUID>) uuidsField.get(reward);

                if (uuids != null) {
                    // Clear any random UUIDs.
                    uuids.clear();

                    // Generate a pool of stable UUIDs from the unique JSON path.
                    // Pufferfish adds modifiers by index, so we provide a stable UUID for each
                    // possible index.
                    // 100 should be more than enough for any reasonable skill level.
                    String path = element.getPath().toString();
                    for (int i = 0; i < 100; i++) {
                        String seed = path + "_level_" + i;
                        uuids.add(java.util.UUID.nameUUIDFromBytes(seed.getBytes()));
                    }

                    SkillLevelingMod.getInstance().getLogger()
                            .debug("[SYNC] Injected 100 stable UUIDs for attribute reward at " + path);
                }
            } catch (Exception e) {
                SkillLevelingMod.getInstance().getLogger()
                        .error("Failed to inject stable UUIDs into AttributeReward: " + e.getMessage());
            }
        }
    }
}
