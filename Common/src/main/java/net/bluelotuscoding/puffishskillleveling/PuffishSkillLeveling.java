package net.bluelotuscoding.puffishskillleveling;

import net.bluelotuscoding.puffishskillleveling.reward.PerLevelRewardsReward;

/**
 * Common entry point for registering addon features.
 */
public final class PuffishSkillLeveling {
    public static final String MOD_ID = "puffish_skill_leveling";

    /**
     * Registers custom rewards with the base Skills API.
     */
    public static void init() {
        PerLevelRewardsReward.register();
    }

    private PuffishSkillLeveling() {
        // utility class
    }
}
