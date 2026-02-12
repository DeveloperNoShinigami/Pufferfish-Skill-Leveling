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
import net.puffish.skillsmod.util.LegacyUtils;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

import java.util.ArrayList;
import java.util.List;

public class ToggleReward implements Reward {
    public static final Identifier ID = SkillLevelingMod.createIdentifier("toggle");

    private final List<SkillRewardConfig> enableRewards;
    private final List<SkillRewardConfig> disableRewards;

    private final java.util.Map<java.util.UUID, Boolean> lastActiveState = new java.util.HashMap<>();

    public void initializeState(java.util.UUID uuid, boolean active) {
        lastActiveState.put(uuid, active);
    }

    public ToggleReward(List<SkillRewardConfig> enableRewards, List<SkillRewardConfig> disableRewards) {
        this.enableRewards = enableRewards;
        this.disableRewards = disableRewards;
    }

    private Identifier cachedCategoryId;
    private String cachedSkillId;

    public void setCachedCategoryId(Identifier categoryId) {
        this.cachedCategoryId = categoryId;
    }

    public void setCachedSkillId(String skillId) {
        this.cachedSkillId = skillId;
    }

    @Override
    public void update(RewardUpdateContext context) {
        var player = context.getPlayer();
        var uuid = player.getUuid();

        // STRICT LOOKUP: Ignore context.getCount() (which is 1 if skill is unlocked).
        // Instead, query the authoritative DataManager state.
        boolean isActive = false;
        if (cachedCategoryId != null && cachedSkillId != null) {
            isActive = SkillLevelingMod.getInstance().getSkillLevelingManager().getDataManager().isToggleActive(player,
                    cachedCategoryId, cachedSkillId);
        }

        boolean wasActive = lastActiveState.getOrDefault(uuid, false);
        boolean transition = isActive != wasActive;

        if (transition) {
            lastActiveState.put(uuid, isActive);
        }

        // 1. Update Enable Rewards (Handles Attributes/Effects ON and OFF)
        // If Active: count=1 (Apply modifiers)
        // If Inactive: count=0 (Remove modifiers)
        int enableCount = isActive ? 1 : 0;

        boolean enableAction;
        if (transition) {
            // FIX: Only set action=true if we are effectively ENABLING.
            // When disabling, we want count=0 (to clear effects) but action=false
            // (to prevent commands from firing).
            enableAction = isActive;
        } else {
            enableAction = context.isAction();
        }

        for (var reward : enableRewards) {
            reward.instance().update(new RewardUpdateContextImpl(player, enableCount, enableAction));
        }

        // 2. Update Disable Rewards (One-shot triggers on disable)
        if (transition && !isActive) {
            for (var reward : disableRewards) {
                // Force action=true when state changes to disabled
                reward.instance().update(new RewardUpdateContextImpl(player, 1, true));
            }
        }
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        var disposeContext = new net.puffish.skillsmod.util.DisposeContext(context.getServer());
        enableRewards.forEach(r -> r.dispose(disposeContext));
        disableRewards.forEach(r -> r.dispose(disposeContext));
        lastActiveState.clear();
    }

    public static void register() {
        SkillsAPI.registerReward(ID, ToggleReward::parse);
    }

    static Result<ToggleReward, Problem> parse(RewardConfigContext context) {
        return context.getData().andThen(JsonElement::getAsObject)
                .andThen(LegacyUtils.wrapNoUnused(obj -> parse(obj, context), context));
    }

    static Result<ToggleReward, Problem> parse(JsonObject rootObject, ConfigContext context) {
        var problems = new ArrayList<Problem>();

        List<SkillRewardConfig> enableRewards = new ArrayList<>();
        rootObject.get("enable_rewards").getSuccess().ifPresent(element -> {
            element.getAsArray().ifSuccess(arr -> {
                arr.getAsList((i, e) -> SkillRewardConfig.parse(e, context))
                        .mapFailure(Problem::combine)
                        .ifSuccess(enableRewards::addAll)
                        .ifFailure(problems::add);
            });
        });

        List<SkillRewardConfig> disableRewards = new ArrayList<>();
        rootObject.get("disable_rewards").getSuccess().ifPresent(element -> {
            element.getAsArray().ifSuccess(arr -> {
                arr.getAsList((i, e) -> SkillRewardConfig.parse(e, context))
                        .mapFailure(Problem::combine)
                        .ifSuccess(disableRewards::addAll)
                        .ifFailure(problems::add);
            });
        });

        if (!problems.isEmpty()) {
            return Result.failure(Problem.combine(problems));
        }

        return Result.success(new ToggleReward(enableRewards, disableRewards));
    }
}
