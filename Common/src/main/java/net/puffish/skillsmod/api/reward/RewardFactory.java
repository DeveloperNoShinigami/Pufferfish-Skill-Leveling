package net.puffish.skillsmod.api.reward;

import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

public interface RewardFactory {
	Result<? extends Reward, Problem> create(RewardConfigContext context);
}
