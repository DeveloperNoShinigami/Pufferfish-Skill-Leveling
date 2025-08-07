package net.bluelotuscoding.puffishskillleveling.api.reward;

public interface Reward {
	void update(RewardUpdateContext context);

	void dispose(RewardDisposeContext context);
}
