package net.puffish.skillsmod.config;

import net.minecraft.server.MinecraftServer;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.config.skill.SkillDefinitionConfig;
import net.puffish.skillsmod.config.skill.SkillDefinitionsConfig;
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

    @Test
    public void testParentDescriptionMerge() {
        String json = """
                {
                  \"parent\": {
                    \"title\": {\"text\": \"Parent\"},
                    \"icon\": {\"type\": \"texture\", \"data\": {\"texture\": \"minecraft:stone\"}},
                    \"descriptions\": [\"A\"],
                    \"extra_descriptions\": [\"EA\"]
                  },
                  \"child\": {
                    \"title\": {\"text\": \"Child\"},
                    \"icon\": {\"type\": \"texture\", \"data\": {\"texture\": \"minecraft:stone\"}},
                    \"parent\": \"parent\",
                    \"merge_description\": true,
                    \"descriptions\": [\"B\"],
                    \"extra_descriptions\": [\"EB\"]
                  }
                }
                """;

        var element = JsonElement.parseString(json, JsonPath.create("test"))
                .getSuccess().orElseThrow();

        var result = SkillDefinitionsConfig.parse(element, new DummyContext());
        Assertions.assertTrue(result.getSuccess().isPresent(),
                result.getFailure().map(Object::toString).orElse("Unexpected failure"));
        var defs = result.getSuccess().orElseThrow();
        var child = defs.getById("child").orElseThrow();
        Assertions.assertEquals(2, child.descriptions().size());
        Assertions.assertEquals(2, child.extraDescriptions().size());
        Assertions.assertEquals("A", child.descriptions().get(0).getString());
        Assertions.assertEquals("EA", child.extraDescriptions().get(0).getString());
    }
}
