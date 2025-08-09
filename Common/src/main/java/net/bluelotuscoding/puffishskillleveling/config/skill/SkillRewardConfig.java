package net.bluelotuscoding.puffishskillleveling.config.skill;

import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.api.config.ConfigContext;
import net.bluelotuscoding.puffishskillleveling.api.json.BuiltinJson;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonElement;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonObject;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonPath;
import net.bluelotuscoding.puffishskillleveling.api.reward.Reward;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;
import net.bluelotuscoding.puffishskillleveling.impl.rewards.RewardConfigContextImpl;
import net.bluelotuscoding.puffishskillleveling.impl.rewards.RewardDisposeContextImpl;
import net.bluelotuscoding.puffishskillleveling.reward.RewardRegistry;
import net.bluelotuscoding.puffishskillleveling.reward.builtin.DummyReward;
import net.bluelotuscoding.puffishskillleveling.util.DisposeContext;
import net.bluelotuscoding.puffishskillleveling.util.LegacyUtils;

import java.util.ArrayList;

public record SkillRewardConfig(
		Identifier type,
		Reward instance
) {

	public static Result<SkillRewardConfig, Problem> parse(JsonElement rootElement, ConfigContext context) {
		return rootElement.getAsObject().andThen(
				LegacyUtils.wrapNoUnused(rootObject -> parse(rootObject, context), context)
		);
	}

	public static Result<SkillRewardConfig, Problem> parse(JsonObject rootObject, ConfigContext context) {
		var problems = new ArrayList<Problem>();

		var optTypeElement = rootObject.get("type")
				.ifFailure(problems::add)
				.getSuccess();

		var optType = optTypeElement.flatMap(
				typeElement -> BuiltinJson.parseIdentifier(typeElement)
						.ifFailure(problems::add)
						.getSuccess()
		);

		var maybeDataElement = rootObject.get("data");

		var required = rootObject.get("required")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsBoolean()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(true);

		if (problems.isEmpty()) {
			return build(
					optType.orElseThrow(),
					maybeDataElement,
					rootObject.getPath().getObject("type"),
					context
			).orElse(problem -> {
				if (required) {
					return Result.failure(problem);
				} else {
					context.emitWarning(problem.toString());
					return Result.success(new SkillRewardConfig(DummyReward.ID, new DummyReward()));
				}
			});
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	private static Result<SkillRewardConfig, Problem> build(Identifier type, Result<JsonElement, Problem> maybeDataElement, JsonPath typePath, ConfigContext context) {
		return RewardRegistry.getFactory(type)
				.map(factory -> factory.create(new RewardConfigContextImpl(context, maybeDataElement))
						.mapSuccess(instance -> new SkillRewardConfig(type, instance))
				)
				.orElseGet(() -> Result.failure(typePath.createProblem("Expected a valid reward type")));
	}

	public void dispose(DisposeContext context) {
		this.instance.dispose(new RewardDisposeContextImpl(context));
	}


}
