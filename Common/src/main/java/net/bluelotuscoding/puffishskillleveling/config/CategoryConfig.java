package net.bluelotuscoding.puffishskillleveling.config;

import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.api.config.ConfigContext;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonElement;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;
import net.bluelotuscoding.puffishskillleveling.config.experience.ExperienceConfig;
import net.bluelotuscoding.puffishskillleveling.config.skill.SkillConnectionsConfig;
import net.bluelotuscoding.puffishskillleveling.config.skill.SkillDefinitionsConfig;
import net.bluelotuscoding.puffishskillleveling.config.skill.SkillsConfig;
import net.bluelotuscoding.puffishskillleveling.util.DisposeContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;

public record CategoryConfig(
		Identifier id,
		GeneralConfig general,
		SkillDefinitionsConfig definitions,
		SkillsConfig skills,
		SkillConnectionsConfig connections,
		Optional<ExperienceConfig> experience
) {

	public static Result<CategoryConfig, Problem> parse(
			Identifier id,
			JsonElement generalElement,
			JsonElement definitionsElement,
			JsonElement skillsElement,
			JsonElement connectionsElement,
			Optional<JsonElement> optExperienceElement,
			ConfigContext context
	) {
		var problems = new ArrayList<Problem>();

		var optGeneral = GeneralConfig.parse(generalElement, context)
				.ifFailure(problems::add)
				.getSuccess();

		var optExperience = optExperienceElement
				.flatMap(experience -> ExperienceConfig.parse(experience, context)
						.ifFailure(problems::add)
						.getSuccess()
						.flatMap(Function.identity())
				);

		var optDefinitions = SkillDefinitionsConfig.parse(definitionsElement, context)
				.ifFailure(problems::add)
				.getSuccess();

		var optSkills = optDefinitions.flatMap(
				definitions -> SkillsConfig.parse(skillsElement, definitions, context)
						.ifFailure(problems::add)
						.getSuccess()
		);

                var optConnections = optSkills.flatMap(
                                skills -> SkillConnectionsConfig.parse(connectionsElement, skills, context)
                                                .ifFailure(problems::add)
                                                .getSuccess()
                );

                if (problems.isEmpty()) {
                        var definitionsCfg = optDefinitions.orElseThrow();
                        var skillsCfg = optSkills.orElseThrow();

                        var usedDefinitions = new HashSet<String>();
                        skillsCfg.getAll().forEach(skill -> {
                                if (!usedDefinitions.add(skill.definitionId())) {
                                        problems.add(Problem.message("Skills must not share definitions"));
                                }
                        });
                        definitionsCfg.getAll().forEach(definition -> {
                                if (!usedDefinitions.contains(definition.id())) {
                                        problems.add(Problem.message("Definition '" + definition.id() + "' is not used by any skill"));
                                }
                        });
                        if (problems.isEmpty()) {
                                return Result.success(new CategoryConfig(
                                                id,
                                                optGeneral.orElseThrow(),
                                                definitionsCfg,
                                                skillsCfg,
                                                optConnections.orElseThrow(),
                                                optExperience
                                ));
                        }
                }
                return Result.failure(Problem.combine(problems));
        }

	public void dispose(DisposeContext context) {
		definitions.dispose(context);
		experience.ifPresent(experience -> experience.dispose(context));
	}

}
