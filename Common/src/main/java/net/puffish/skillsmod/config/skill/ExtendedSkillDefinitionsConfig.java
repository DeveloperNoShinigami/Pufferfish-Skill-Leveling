package net.puffish.skillsmod.config.skill;

import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.impl.json.JsonObjectImpl;
import net.puffish.skillsmod.util.DisposeContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Collection of {@link ExtendedSkillDefinitionConfig} objects. The base
 * {@link SkillDefinitionsConfig} parsing is delegated to ensure the
 * original validation rules are applied.
 */
public class ExtendedSkillDefinitionsConfig {
    private final SkillDefinitionsConfig base;
    private final Map<String, ExtendedSkillDefinitionConfig> definitions;

    private ExtendedSkillDefinitionsConfig(SkillDefinitionsConfig base, Map<String, ExtendedSkillDefinitionConfig> definitions) {
        this.base = base;
        this.definitions = definitions;
    }

    public static Result<ExtendedSkillDefinitionsConfig, Problem> parse(JsonElement rootElement, ConfigContext context) {
        return rootElement.getAsObject().andThen(obj -> parse(obj, context));
    }

    public static Result<ExtendedSkillDefinitionsConfig, Problem> parse(JsonObject rootObject, ConfigContext context) {
        var problems = new ArrayList<Problem>();

        // Parse extended definitions from a copy so we can reuse the original object for the base parser
        var copy = new JsonObjectImpl(rootObject.getJson().deepCopy(), rootObject.getPath());
        var optExt = copy.getAsMap((id, element) -> ExtendedSkillDefinitionConfig.parse(id, element, context))
                .mapFailure(map -> Problem.combine(map.values()))
                .ifFailure(problems::add)
                .getSuccess();

        var optBase = SkillDefinitionsConfig.parse(rootObject, context)
                .ifFailure(problems::add)
                .getSuccess();

        if (problems.isEmpty()) {
            return Result.success(new ExtendedSkillDefinitionsConfig(optBase.orElseThrow(), optExt.orElseThrow()));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    public Optional<ExtendedSkillDefinitionConfig> getById(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public Collection<ExtendedSkillDefinitionConfig> getAll() {
        return definitions.values();
    }

    public SkillDefinitionsConfig base() {
        return base;
    }

    public void dispose(DisposeContext context) {
        base.dispose(context);
    }
}
