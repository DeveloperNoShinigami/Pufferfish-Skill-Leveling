package net.puffish.skill_leveling.reward.builtin;

import net.minecraft.util.Identifier;
import net.puffish.skill_leveling.SkillsMod;
import net.puffish.skill_leveling.api.SkillsAPI;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.json.JsonObject;
import net.puffish.skill_leveling.api.reward.Reward;
import net.puffish.skill_leveling.api.reward.RewardConfigContext;
import net.puffish.skill_leveling.api.reward.RewardDisposeContext;
import net.puffish.skill_leveling.api.reward.RewardUpdateContext;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;
import net.puffish.skill_leveling.util.LegacyUtils;

import java.util.ArrayList;

public class TagReward implements Reward {
	public static final Identifier ID = SkillsMod.createIdentifier("tag");

	private final String tag;

	private TagReward(String tag) {
		this.tag = tag;
	}

	public static void register() {
		SkillsAPI.registerReward(
				ID,
				TagReward::parse
		);
	}

	private static Result<TagReward, Problem> parse(RewardConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(LegacyUtils.wrapNoUnused(TagReward::parse, context));
	}

	private static Result<TagReward, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		var optTag = rootObject.getString("tag")
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new TagReward(
					optTag.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public void update(RewardUpdateContext context) {
		var player = context.getPlayer();
		if (context.getCount() > 0) {
			player.addCommandTag(tag);
		} else {
			player.removeScoreboardTag(tag);
		}
	}

	@Override
	public void dispose(RewardDisposeContext context) {
		// Nothing to do.
	}
}
