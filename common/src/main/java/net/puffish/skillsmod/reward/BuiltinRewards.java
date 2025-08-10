package net.puffish.skillsmod.reward;

import net.puffish.skillsmod.reward.builtin.AttributeReward;
import net.puffish.skillsmod.reward.builtin.CommandReward;
import net.puffish.skillsmod.reward.builtin.PointsReward;
import net.puffish.skillsmod.reward.builtin.ScoreboardReward;
import net.puffish.skillsmod.reward.builtin.TagReward;
import net.puffish.skillsmod.reward.builtin.PerLevelRewardsReward;

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
