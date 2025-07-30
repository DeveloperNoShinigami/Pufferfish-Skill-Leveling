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

    @Test
    public void testMultiLineMergeDescription() {
        String json = """
                {
                  \"base\": {
                    \"title\": {\"text\": \"Base\"},
                    \"icon\": {\"type\": \"texture\", \"data\": {\"texture\": \"minecraft:stone\"}},
                    \"descriptions\": [\"A1\", \"A2\"],
                    \"extra_descriptions\": [\"EA1\", \"EA2\"]
                  },
                  \"child\": {
                    \"title\": {\"text\": \"Child\"},
                    \"icon\": {\"type\": \"texture\", \"data\": {\"texture\": \"minecraft:stone\"}},
                    \"parent\": \"base\",
                    \"merge_description\": true,
                    \"descriptions\": [\"B1\", \"B2\"],
                    \"extra_descriptions\": [\"EB1\", \"EB2\"]
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
        Assertions.assertEquals(4, child.descriptions().size());
        Assertions.assertEquals(4, child.extraDescriptions().size());
        Assertions.assertEquals("A1", child.descriptions().get(0).getString());
        Assertions.assertEquals("A2", child.descriptions().get(1).getString());
        Assertions.assertEquals("B1", child.descriptions().get(2).getString());
        Assertions.assertEquals("B2", child.descriptions().get(3).getString());
        Assertions.assertEquals("EA1", child.extraDescriptions().get(0).getString());
        Assertions.assertEquals("EA2", child.extraDescriptions().get(1).getString());
        Assertions.assertEquals("EB1", child.extraDescriptions().get(2).getString());
        Assertions.assertEquals("EB2", child.extraDescriptions().get(3).getString());
    }
}
