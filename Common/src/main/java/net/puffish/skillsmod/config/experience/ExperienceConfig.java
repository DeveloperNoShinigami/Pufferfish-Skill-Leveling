package net.puffish.skillsmod.config.experience;

import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.util.DisposeContext;

import java.util.Optional;

public record ExperienceConfig() {
    public static Result<Optional<ExperienceConfig>, Problem> parse(JsonElement rootElement, ConfigContext context) {
        return rootElement.getAsObject().mapSuccess(obj -> Optional.of(new ExperienceConfig()));
    }

    public static Result<Optional<ExperienceConfig>, Problem> parse(JsonObject rootObject, ConfigContext context) {
        return Result.success(Optional.of(new ExperienceConfig()));
    }

    public void dispose(DisposeContext context) {
        // no-op
    }
}
