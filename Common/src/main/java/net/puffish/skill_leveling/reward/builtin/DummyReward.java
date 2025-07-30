package net.puffish.skill_leveling.reward.builtin;

import net.minecraft.util.Identifier;
import net.puffish.skill_leveling.SkillsMod;
import net.puffish.skill_leveling.api.reward.Reward;
import net.puffish.skill_leveling.api.reward.RewardDisposeContext;
import net.puffish.skill_leveling.api.reward.RewardUpdateContext;

public class DummyReward implements Reward {
	public static final Identifier ID = SkillsMod.createIdentifier("dummy");

	@Override
	public void update(RewardUpdateContext context) { }

	@Override
	public void dispose(RewardDisposeContext context) { }
}
