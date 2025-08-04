package net.bluelotuscoding.puffishskillleveling;

import net.bluelotuscoding.puffishskillleveling.reward.PerLevelRewardsReward;
import net.bluelotuscoding.puffishskillleveling.experience.source.BuiltinExperienceSources;

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
        BuiltinExperienceSources.register();
    }

    private PuffishSkillLeveling() {
        // utility class
    }
}
