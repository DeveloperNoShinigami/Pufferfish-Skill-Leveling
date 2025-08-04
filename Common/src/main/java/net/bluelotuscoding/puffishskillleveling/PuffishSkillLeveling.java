package net.bluelotuscoding.puffishskillleveling;

import net.bluelotuscoding.puffishskillleveling.reward.AttributeReward;
import net.bluelotuscoding.puffishskillleveling.reward.CommandReward;
import net.bluelotuscoding.puffishskillleveling.reward.DummyReward;
import net.bluelotuscoding.puffishskillleveling.reward.PerLevelRewardsReward;
import net.bluelotuscoding.puffishskillleveling.reward.PointsReward;
import net.bluelotuscoding.puffishskillleveling.reward.ScoreboardReward;
import net.bluelotuscoding.puffishskillleveling.reward.TagReward;

/**
 * Common entry point for registering addon features.
 */
public final class PuffishSkillLeveling {
    public static final String MOD_ID = "puffish_skill_leveling";

    /**
     * Registers custom rewards with the base Skills API.
     */
    public static void init() {
        AttributeReward.register();
        CommandReward.register();
        DummyReward.register();
        PerLevelRewardsReward.register();
        PointsReward.register();
        ScoreboardReward.register();
        TagReward.register();
    }

    private PuffishSkillLeveling() {
        // utility class
    }
}
