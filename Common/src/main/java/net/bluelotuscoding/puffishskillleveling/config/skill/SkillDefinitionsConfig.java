package net.bluelotuscoding.puffishskillleveling.config.skill;

import net.bluelotuscoding.puffishskillleveling.api.config.ConfigContext;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonElement;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonObject;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;
import net.bluelotuscoding.puffishskillleveling.util.DisposeContext;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class SkillDefinitionsConfig {
	private final Map<String, SkillDefinitionConfig> definitions;

	private SkillDefinitionsConfig(Map<String, SkillDefinitionConfig> definitions) {
		this.definitions = definitions;
	}

	public static Result<SkillDefinitionsConfig, Problem> parse(JsonElement rootElement, ConfigContext context) {
		return rootElement.getAsObject().andThen(rootObject -> parse(rootObject, context));
	}

       public static Result<SkillDefinitionsConfig, Problem> parse(JsonObject rootObject, ConfigContext context) {
               return rootObject.getAsMap((id, element) -> SkillDefinitionConfig.parse(id, element, context))
                               .mapFailure(problems -> Problem.combine(problems.values()))
                               .mapSuccess(SkillDefinitionsConfig::new);
       }

	public Optional<SkillDefinitionConfig> getById(String id) {
		return Optional.ofNullable(definitions.get(id));
	}

	public Collection<SkillDefinitionConfig> getAll() {
		return definitions.values();
	}

	public void dispose(DisposeContext context) {
		for (var definition : definitions.values()) {
			definition.dispose(context);
		}
	}
}
