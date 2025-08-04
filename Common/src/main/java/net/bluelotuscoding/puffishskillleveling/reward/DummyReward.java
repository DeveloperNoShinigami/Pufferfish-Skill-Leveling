package net.bluelotuscoding.puffishskillleveling.reward;

import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

/**
 * No-op reward used as a placeholder.
 */
public class DummyReward implements Reward {
    public static final ResourceLocation ID = SkillsMod.createIdentifier("dummy");

    public static void register() {
        SkillsAPI.registerReward(ID, DummyReward::parse);
    }

    private static Result<DummyReward, Problem> parse(RewardConfigContext context) {
        return Result.success(new DummyReward());
    }

    @Override
    public void update(RewardUpdateContext context) {
        // no-op
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        // no-op
    }
}
