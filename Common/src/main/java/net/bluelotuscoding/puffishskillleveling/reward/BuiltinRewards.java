package net.bluelotuscoding.puffishskillleveling.reward;

import net.bluelotuscoding.puffishskillleveling.reward.builtin.AttributeReward;
import net.bluelotuscoding.puffishskillleveling.reward.builtin.CommandReward;
import net.bluelotuscoding.puffishskillleveling.reward.builtin.PointsReward;
import net.bluelotuscoding.puffishskillleveling.reward.builtin.ScoreboardReward;
import net.bluelotuscoding.puffishskillleveling.reward.builtin.TagReward;
import net.bluelotuscoding.puffishskillleveling.reward.builtin.PerLevelRewardsReward;

public class BuiltinRewards {
	public static void register() {
		AttributeReward.register();
		CommandReward.register();
		PointsReward.register();
		ScoreboardReward.register();
		TagReward.register();
		PerLevelRewardsReward.register();
	}
}
