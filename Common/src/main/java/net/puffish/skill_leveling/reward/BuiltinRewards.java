package net.puffish.skill_leveling.reward;

import net.puffish.skill_leveling.reward.builtin.AttributeReward;
import net.puffish.skill_leveling.reward.builtin.CommandReward;
import net.puffish.skill_leveling.reward.builtin.PointsReward;
import net.puffish.skill_leveling.reward.builtin.ScoreboardReward;
import net.puffish.skill_leveling.reward.builtin.TagReward;
import net.puffish.skill_leveling.reward.builtin.PerLevelRewardsReward;

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
