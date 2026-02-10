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

    public ToggleReward(List<SkillRewardConfig> enableRewards, List<SkillRewardConfig> disableRewards) {
        this.enableRewards = enableRewards;
        this.disableRewards = disableRewards;
    }

    @Override
    public void update(RewardUpdateContext context) {
        var player = context.getPlayer();
        int count = context.getCount();
        boolean action = context.isAction();

        if (count > 0) {
            // Skill is enabled
            for (var reward : enableRewards) {
                reward.instance().update(new RewardUpdateContextImpl(player, 1, action));
            }
            for (var reward : disableRewards) {
                reward.instance().update(new RewardUpdateContextImpl(player, 0, false));
            }
        } else {
            // Skill is disabled
            for (var reward : enableRewards) {
                reward.instance().update(new RewardUpdateContextImpl(player, 0, action));
            }
            for (var reward : disableRewards) {
                reward.instance().update(new RewardUpdateContextImpl(player, 1, action));
            }
        }
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        var disposeContext = new net.puffish.skillsmod.util.DisposeContext(context.getServer());
        enableRewards.forEach(r -> r.dispose(disposeContext));
        disableRewards.forEach(r -> r.dispose(disposeContext));
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
            }).ifFailure(problems::add);
        });

        List<SkillRewardConfig> disableRewards = new ArrayList<>();
        rootObject.get("disable_rewards").getSuccess().ifPresent(element -> {
            element.getAsArray().ifSuccess(arr -> {
                arr.getAsList((i, e) -> SkillRewardConfig.parse(e, context))
                        .mapFailure(Problem::combine)
                        .ifSuccess(disableRewards::addAll)
                        .ifFailure(problems::add);
            }).ifFailure(problems::add);
        });

        if (problems.isEmpty()) {
            return Result.success(new ToggleReward(enableRewards, disableRewards));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }
}
