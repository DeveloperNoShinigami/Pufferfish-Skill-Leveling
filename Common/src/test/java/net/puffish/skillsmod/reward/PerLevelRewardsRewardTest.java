package net.puffish.skillsmod.reward;

import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.reward.builtin.PerLevelRewardsReward;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PerLevelRewardsRewardTest {
    private static class DummyContext implements ConfigContext {
        @Override
        public net.minecraft.server.MinecraftServer getServer() {
            return null;
        }
        @Override
        public void emitWarning(String message) {
        }
    }

    @Test
    public void testParsePointsPerLevel() {
        String json = "{\"levels\":{},\"points_per_level\":2,\"skill_id\":\"id\",\"max_level\":3}";
        var element = JsonElement.parseString(json, JsonPath.create("test"))
                .getSuccess().orElseThrow();
        var result = PerLevelRewardsReward.parse(element.getAsObject().orElseThrow(), new DummyContext());
        Assertions.assertTrue(result.getSuccess().isPresent(),
                result.getFailure().map(Object::toString).orElse("Unexpected failure"));
        var reward = result.getSuccess().orElseThrow();
        Assertions.assertEquals(2, reward.getPointsPerLevel());
        Assertions.assertEquals(3, reward.getMaxLevel());
        Assertions.assertEquals("id", reward.getSkillId());
    }
}
