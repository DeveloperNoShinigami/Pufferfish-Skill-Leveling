package net.puffish.skillsmod.reward;

import net.minecraft.server.MinecraftServer;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.reward.builtin.PerLevelRewardsReward;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PerLevelRewardsRewardTest {
    private static class DummyContext implements ConfigContext {
        @Override
        public MinecraftServer getServer() {
            return null;
        }

        @Override
        public void emitWarning(String message) {
            // ignore
        }
    }

    @Test
    public void testParseOptionalFields() {
        String json = """
                {
                  \"levels\": {},
                  \"skill_id\": \"test_skill\",
                  \"max_level\": 2,
                  \"points_per_level\": 3
                }
                """;
        var element = JsonElement.parseString(json, JsonPath.create("test"))
                .getSuccess().orElseThrow();

        var result = PerLevelRewardsReward.parse(element.getAsObject().getSuccess().orElseThrow(), new DummyContext());
        Assertions.assertTrue(result.getSuccess().isPresent(),
                result.getFailure().map(Object::toString).orElse("Unexpected failure"));
    }
}
