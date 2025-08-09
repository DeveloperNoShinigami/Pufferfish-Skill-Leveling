package net.bluelotuscoding.puffishskillleveling.reward.builtin;

import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.SkillsMod;
import net.bluelotuscoding.puffishskillleveling.api.reward.Reward;
import net.bluelotuscoding.puffishskillleveling.api.reward.RewardDisposeContext;
import net.bluelotuscoding.puffishskillleveling.api.reward.RewardUpdateContext;

public class DummyReward implements Reward {
	public static final Identifier ID = SkillsMod.createIdentifier("dummy");

	@Override
	public void update(RewardUpdateContext context) { }

	@Override
	public void dispose(RewardDisposeContext context) { }
}
