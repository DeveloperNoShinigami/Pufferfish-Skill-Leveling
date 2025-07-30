package net.puffish.skill_leveling.api.reward;

import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;

public interface RewardFactory {
	Result<? extends Reward, Problem> create(RewardConfigContext context);
}
