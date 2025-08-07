package net.puffish.skillsmod.config.skill;

import net.minecraft.server.MinecraftServer;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.reward.builtin.PerLevelRewardsReward;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SkillDefinitionConfigTest {
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

    static {
        try {
            PerLevelRewardsReward.register();
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void testMaxLevelFromPerLevelReward() {
        String json = """
{
  \"title\": \"Test\",
  \"icon\": { \"type\": \"texture\", \"data\": { \"texture\": \"minecraft:stone\" } },
  \"points_per_level\": 0,
  \"rewards\": [
    {
      \"type\": \"puffish_skill_leveling:per_level_rewards\",
      \"data\": {
        \"skill_id\": \"skill\",
        \"levels\": { \"1\": [], \"2\": [], \"3\": [] }
      }
    }
  ]
}
""";
        var element = JsonElement.parseString(json, JsonPath.create("test")).getSuccess().orElseThrow();
        var result = SkillDefinitionConfig.parse("skill", element, new DummyContext());
        Assertions.assertEquals(3, result.getSuccess().orElseThrow().maxLevels());
    }

    @Test
    public void testMaxLevelOverridesRoot() {
        String json = """
{
  \"title\": \"Test\",
  \"icon\": { \"type\": \"texture\", \"data\": { \"texture\": \"minecraft:stone\" } },
  \"points_per_level\": 0,
  \"max_skill_level\": 1,
  \"rewards\": [
    {
      \"type\": \"puffish_skill_leveling:per_level_rewards\",
      \"data\": {
        \"skill_id\": \"skill\",
        \"max_skill_level\": 3,
        \"levels\": { \"1\": [], \"2\": [], \"3\": [] }
      }
    }
  ]
}
""";
        var element = JsonElement.parseString(json, JsonPath.create("test")).getSuccess().orElseThrow();
        var result = SkillDefinitionConfig.parse("skill", element, new DummyContext());
        Assertions.assertEquals(3, result.getSuccess().orElseThrow().maxLevels());
    }
}
