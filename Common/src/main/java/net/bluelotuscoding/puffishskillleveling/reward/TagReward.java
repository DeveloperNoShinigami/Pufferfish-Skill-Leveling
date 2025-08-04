package net.bluelotuscoding.puffishskillleveling.reward;

import java.util.ArrayList;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
 * Reward that toggles a scoreboard tag on the player.
 */
public class TagReward implements Reward {
    public static final ResourceLocation ID = SkillsMod.createIdentifier("tag");

    private final String tag;

    private TagReward(String tag) {
        this.tag = tag;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, TagReward::parse);
    }

    private static Result<TagReward, Problem> parse(RewardConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(LegacyUtils.wrapNoUnused(TagReward::parse, context));
    }

    private static Result<TagReward, Problem> parse(JsonObject rootObject) {
        var problems = new ArrayList<Problem>();

        Optional<String> optTag = rootObject.getString("tag")
                .ifFailure(problems::add)
                .getSuccess();

        if (problems.isEmpty()) {
            return Result.success(new TagReward(optTag.orElseThrow()));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    @Override
    public void update(RewardUpdateContext context) {
        ServerPlayer player = context.getPlayer();
        if (context.getCount() > 0) {
            player.addTag(tag);
        } else {
            player.removeTag(tag);
        }
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        // nothing to clean up
    }
}
