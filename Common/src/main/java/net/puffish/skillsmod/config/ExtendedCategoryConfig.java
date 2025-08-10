package net.puffish.skillsmod.config;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.config.experience.ExperienceConfig;
import net.puffish.skillsmod.config.skill.ExtendedSkillDefinitionsConfig;
import net.puffish.skillsmod.config.skill.SkillConnectionsConfig;
import net.puffish.skillsmod.config.skill.SkillsConfig;
import net.puffish.skillsmod.util.DisposeContext;
import net.puffish.skillsmod.impl.json.JsonElementImpl;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Wrapper over {@link CategoryConfig} that exposes the extended
 * {@link ExtendedSkillDefinitionsConfig}. Base parsing and validation
 * are delegated to {@link CategoryConfig} to keep behaviour consistent
 * with the original implementation.
 */
public record ExtendedCategoryConfig(
        Identifier id,
        GeneralConfig general,
        ExtendedSkillDefinitionsConfig definitions,
        SkillsConfig skills,
        SkillConnectionsConfig connections,
        Optional<ExperienceConfig> experience
) {

    public static Result<ExtendedCategoryConfig, Problem> parse(
            Identifier id,
            JsonElement generalElement,
            JsonElement definitionsElement,
            JsonElement skillsElement,
            JsonElement connectionsElement,
            Optional<JsonElement> optExperienceElement,
            ConfigContext context
    ) {
        var problems = new ArrayList<Problem>();

        // Parse extended definitions on a copy to avoid interfering with base parsing
        var definitionsCopy = new JsonElementImpl(definitionsElement.getJson().deepCopy(), definitionsElement.getPath());
        var optDefinitions = ExtendedSkillDefinitionsConfig.parse(definitionsCopy, context)
                .ifFailure(problems::add)
                .getSuccess();

        var optBase = CategoryConfig.parse(
                id,
                generalElement,
                definitionsElement,
                skillsElement,
                connectionsElement,
                optExperienceElement,
                context
        ).ifFailure(problems::add).getSuccess();

        if (problems.isEmpty()) {
            var base = optBase.orElseThrow();
            return Result.success(new ExtendedCategoryConfig(
                    base.id(),
                    base.general(),
                    optDefinitions.orElseThrow(),
                    base.skills(),
                    base.connections(),
                    base.experience()
            ));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    public void dispose(DisposeContext context) {
        definitions.dispose(context);
        experience.ifPresent(exp -> exp.dispose(context));
    }
}

