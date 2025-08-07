package net.bluelotuscoding.puffishskillleveling.api.reward;

import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;

public interface RewardFactory {
	Result<? extends Reward, Problem> create(RewardConfigContext context);
}
