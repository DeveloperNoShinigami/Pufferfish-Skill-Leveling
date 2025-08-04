package net.bluelotuscoding.puffishskillleveling.reward;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.BuiltinJson;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Reward that grants points to a specific category using a temporary source id.
 */
public class PointsReward implements Reward {
    public static final ResourceLocation ID = SkillsMod.createIdentifier("points");
    private static final String PREFIX = "points_reward.";

    private final ResourceLocation categoryId;
    private final int points;
    private final ResourceLocation source;

    private PointsReward(ResourceLocation categoryId, int points, ResourceLocation source) {
        this.categoryId = categoryId;
        this.points = points;
        this.source = source;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, PointsReward::parse);
    }

    private static Result<PointsReward, Problem> parse(RewardConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(obj -> obj.noUnused(PointsReward::parse));
    }

    private static Result<PointsReward, Problem> parse(JsonObject rootObject) {
        var problems = new ArrayList<Problem>();

        Optional<ResourceLocation> optCategory = rootObject.get("category")
                .andThen(BuiltinJson::parseIdentifier)
                .ifFailure(problems::add)
                .getSuccess();

        Optional<Integer> optPoints = rootObject.getInt("points")
                .ifFailure(problems::add)
                .getSuccess();

        if (problems.isEmpty()) {
            var source = SkillsMod.createIdentifier(
                    PREFIX + RandomStringUtils.random(16, "abcdefghijklmnopqrstuvwxyz0123456789"));
            return Result.success(new PointsReward(optCategory.orElseThrow(), optPoints.orElseThrow(), source));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    public static void cleanup(ServerPlayer player) {
        SkillsAPI.streamCategories().forEach(category -> {
            List<ResourceLocation> sources = category.streamPointsSources(player)
                    .filter(src -> src.getNamespace().equals("puffish_skills"))
                    .filter(src -> src.getPath().startsWith(PREFIX))
                    .toList();
            for (var src : sources) {
                category.setPoints(player, src, 0);
            }
        });
    }

    @Override
    public void update(RewardUpdateContext context) {
        SkillsAPI.getCategory(categoryId).ifPresent(category -> {
            int amount = points * context.getCount();
            if (context.isAction()) {
                category.setPoints(context.getPlayer(), source, amount);
            } else {
                category.setPointsSilently(context.getPlayer(), source, amount);
            }
        });
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        SkillsAPI.getCategory(categoryId).ifPresent(category -> {
            context.getServer().getPlayerList().getPlayers()
                    .forEach(player -> category.setPoints(player, source, 0));
        });
    }
}
