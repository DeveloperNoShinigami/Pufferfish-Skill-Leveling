package net.bluelotuscoding.puffishskillleveling.reward.builtin;

import net.minecraft.server.MinecraftServer;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonElement;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonPath;
import net.bluelotuscoding.puffishskillleveling.api.reward.RewardConfigContext;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PerLevelRewardsRewardTest {

	private static class DummyContext implements RewardConfigContext {
	    private final JsonElement data;

	    private DummyContext(JsonElement data) {
	        this.data = data;
	    }

	    @Override
	    public MinecraftServer getServer() {
	        return null;
	    }

	    @Override
	    public void emitWarning(String message) {
	        // ignore
	    }

	    @Override
	    public Result<JsonElement, Problem> getData() {
	        return Result.success(data);
	    }
	}

	private static Result<PerLevelRewardsReward, Problem> parse(String json) {
	    var element = JsonElement.parseString(json, JsonPath.create("test"))
	            .getSuccess().orElseThrow();
	    return PerLevelRewardsReward.parse(new DummyContext(element));
	}

        @Test
        public void testValidValues() {
            String json = "{\"skill_id\":\"s\",\"max_skill_level\":1,\"points_per_level\":0,\"levels\":{\"1\":[]}}";
            var result = parse(json);
            Assertions.assertEquals(1, result.getSuccess().orElseThrow().getMaxLevel());
        }

        @Test
        public void testLevelsInferMaxLevel() {
            String json = "{\"skill_id\":\"s\",\"levels\":{\"1\":[],\"2\":[]}}";
            var result = parse(json);
            var reward = result.getSuccess().orElseThrow();
            Assertions.assertEquals(2, reward.getMaxLevel());
            Assertions.assertEquals(0, reward.getPointsPerLevel());
        }

	@Test
	public void testInvalidMaxLevel() {
            String json = "{\"skill_id\":\"s\",\"max_skill_level\":0,\"points_per_level\":0,\"levels\":{\"1\":[]}}";
	    var result = parse(json);
	    Assertions.assertTrue(result.getFailure().isPresent());
	}

	@Test
	public void testInvalidPointsPerLevel() {
            String json = "{\"skill_id\":\"s\",\"max_skill_level\":1,\"points_per_level\":-1,\"levels\":{\"1\":[]}}";
	    var result = parse(json);
	    Assertions.assertTrue(result.getFailure().isPresent());
	}
}
