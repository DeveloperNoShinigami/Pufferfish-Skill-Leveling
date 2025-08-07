package net.puffish.skillsmod.api;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.reward.RewardFactory;
import net.puffish.skillsmod.reward.RewardRegistry;

public final class SkillsAPI {
    private SkillsAPI() {
    }

    public static final String MOD_ID = "puffish_skill_leveling";

    public static void registerReward(Identifier key, RewardFactory factory) {
        RewardRegistry.register(key, factory);
    }
}
