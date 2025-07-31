package net.puffish.skillsmod.api.reward;

public interface Reward {
	void update(RewardUpdateContext context);

	void dispose(RewardDisposeContext context);
}
