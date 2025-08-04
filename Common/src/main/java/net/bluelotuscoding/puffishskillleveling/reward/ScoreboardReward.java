package net.bluelotuscoding.puffishskillleveling.reward;

import java.util.ArrayList;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.util.LegacyUtils;

/**
 * Reward that sets a scoreboard objective to the current count.
 */
public class ScoreboardReward implements Reward {
    public static final ResourceLocation ID = SkillsMod.createIdentifier("scoreboard");

    private final String objectiveName;

    private ScoreboardReward(String objectiveName) {
        this.objectiveName = objectiveName;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, ScoreboardReward::parse);
    }

    private static Result<ScoreboardReward, Problem> parse(RewardConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(LegacyUtils.wrapNoUnused(root -> parse(root, context), context));
    }

    private static Result<ScoreboardReward, Problem> parse(JsonObject rootObject, RewardConfigContext context) {
        var problems = new ArrayList<Problem>();

        Optional<String> optObjective = rootObject.getString("objective")
                .orElse(LegacyUtils.wrapDeprecated(() -> rootObject.getString("scoreboard"), 3, context))
                .ifFailure(problems::add)
                .getSuccess();

        if (problems.isEmpty()) {
            return Result.success(new ScoreboardReward(optObjective.orElseThrow()));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    @Override
    public void update(RewardUpdateContext context) {
        ServerPlayer player = context.getPlayer();
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective != null) {
            scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective)
                    .setScore(context.getCount());
        }
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        // nothing to clean up
    }
}
