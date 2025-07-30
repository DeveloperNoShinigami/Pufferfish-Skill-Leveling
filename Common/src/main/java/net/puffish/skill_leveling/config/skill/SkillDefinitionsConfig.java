package net.puffish.skill_leveling.config.skill;

import net.puffish.skill_leveling.api.config.ConfigContext;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.json.JsonObject;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;
import net.puffish.skill_leveling.util.DisposeContext;

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
                                .andThen(map -> {
                                        var problems = new java.util.ArrayList<Problem>();
                                        var merged = new java.util.HashMap<String, SkillDefinitionConfig>();
                                        for (var entry : map.entrySet()) {
                                                merge(entry.getKey(), map, merged, new java.util.HashSet<>(), problems);
                                        }
                                        if (problems.isEmpty()) {
                                                return Result.success(new SkillDefinitionsConfig(merged));
                                        } else {
                                                return Result.failure(Problem.combine(problems));
                                        }
                                });
        }

        private static SkillDefinitionConfig merge(String id, Map<String, SkillDefinitionConfig> all,
                                    Map<String, SkillDefinitionConfig> merged,
                                    java.util.Set<String> visiting,
                                    java.util.List<Problem> problems) {
                if (merged.containsKey(id)) {
                        return merged.get(id);
                }
                if (!visiting.add(id)) {
                        problems.add(Problem.message("Cycle in skill definition inheritance at '" + id + "'"));
                        return null;
                }
                var def = all.get(id);
                if (def == null) {
                        problems.add(Problem.message("Unknown skill definition '" + id + "'"));
                        return null;
                }
                java.util.List<net.minecraft.text.Text> descriptions = def.descriptions();
                java.util.List<net.minecraft.text.Text> extra = def.extraDescriptions();
                if (def.mergeDescription() && def.parent().isPresent()) {
                        var parentId = def.parent().get();
                        var parentObj = merge(parentId, all, merged, visiting, problems);
                        if (parentObj != null) {
                                descriptions = new java.util.ArrayList<>(parentObj.descriptions());
                                descriptions.addAll(def.descriptions());
                                extra = new java.util.ArrayList<>(parentObj.extraDescriptions());
                                extra.addAll(def.extraDescriptions());
                        }
                } else if (def.parent().isPresent()) {
                        merge(def.parent().get(), all, merged, visiting, problems);
                }
                visiting.remove(id);

                var mergedDef = new SkillDefinitionConfig(
                                def.id(),
                                def.parent(),
                                def.type(),
                                def.maxLevels(),
                                descriptions,
                                extra,
                                def.title(),
                                def.icon(),
                                def.frame(),
                                def.size(),
                                def.mergeDescription(),
                                def.rewards(),
                                def.cost(),
                                def.requiredSkills(),
                                def.requiredPoints(),
                                def.requiredSpentPoints(),
                                def.requiredExclusions()
                );
                merged.put(id, mergedDef);
                return mergedDef;

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
