package net.puffish.skillsmod.config;

import net.minecraft.server.MinecraftServer;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.config.skill.SkillDefinitionConfig;
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

    @Test
    public void testMergeDescriptionTrue() {
        String json = """
                {
                  \"title\": {\"text\": \"Title\"},
                  \"icon\": {\"type\": \"texture\", \"data\": {\"texture\": \"minecraft:stone\"}},
                  \"merge_description\": true
                }
                """;

        var element = JsonElement.parseString(json, JsonPath.create("test"))
                .getSuccess().orElseThrow();

        var result = SkillDefinitionConfig.parse("id", element, new DummyContext());
        Assertions.assertTrue(result.getSuccess().isPresent(),
                result.getFailure().map(Object::toString).orElse("Unexpected failure"));
        var config = result.getSuccess().orElseThrow();
        Assertions.assertTrue(config.mergeDescription());
    }

}
