package net.puffish.skillsmod.impl.rewards;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;

public interface SkillRewardUpdateContext extends RewardUpdateContext {
    Identifier getCategoryId();
    String getSkillId();
}
