package net.puffish.skill_leveling.api.reward;

public interface Reward {
	void update(RewardUpdateContext context);

	void dispose(RewardDisposeContext context);
}
