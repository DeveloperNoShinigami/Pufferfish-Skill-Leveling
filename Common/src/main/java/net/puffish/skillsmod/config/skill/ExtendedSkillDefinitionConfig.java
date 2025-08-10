package net.puffish.skillsmod.config.skill;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.config.FrameConfig;
import net.puffish.skillsmod.config.IconConfig;
import net.puffish.skillsmod.config.skill.SkillRewardConfig;
import net.puffish.skillsmod.util.DisposeContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Extension of {@link SkillDefinitionConfig} that exposes the additional
 * {@code points_per_level} property used by level rewards. The remaining
 * fields are delegated to the wrapped {@link SkillDefinitionConfig}
 * instance so the original parsing and validation logic is preserved.
 */
public class ExtendedSkillDefinitionConfig {
    private final SkillDefinitionConfig base;
    private final int pointsPerLevel;

    private ExtendedSkillDefinitionConfig(SkillDefinitionConfig base, int pointsPerLevel) {
        this.base = base;
        this.pointsPerLevel = pointsPerLevel;
    }

    public static Result<ExtendedSkillDefinitionConfig, Problem> parse(String id, JsonElement rootElement, ConfigContext context) {
        return rootElement.getAsObject().andThen(obj -> parse(id, obj, context));
    }

    public static Result<ExtendedSkillDefinitionConfig, Problem> parse(String id, JsonObject rootObject, ConfigContext context) {
        var problems = new ArrayList<Problem>();

        // Read the extra points_per_level field while leaving validation to the base parser
        int pointsPerLevel = rootObject.get("points_per_level")
                .getSuccess()
                .flatMap(element -> element.getAsInt().getSuccess())
                .orElse(0);

        var optBase = SkillDefinitionConfig.parse(id, rootObject, context)
                .ifFailure(problems::add)
                .getSuccess();

        if (problems.isEmpty()) {
            return Result.success(new ExtendedSkillDefinitionConfig(optBase.orElseThrow(), pointsPerLevel));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    // ---- Delegated accessors ----

    public String id() {
        return base.id();
    }

    public Identifier type() {
        return base.type();
    }

    public int maxLevels() {
        return base.maxLevels();
    }

    public List<Text> descriptions() {
        return base.descriptions();
    }

    public List<Text> extraDescriptions() {
        return base.extraDescriptions();
    }

    public Text title() {
        return base.title();
    }

    public IconConfig icon() {
        return base.icon();
    }

    public FrameConfig frame() {
        return base.frame();
    }

    public float size() {
        return base.size();
    }

    public boolean mergeDescription() {
        return base.mergeDescription();
    }

    public List<SkillRewardConfig> rewards() {
        return base.rewards();
    }

    public int cost() {
        return base.cost();
    }

    public int requiredSkills() {
        return base.requiredSkills();
    }

    public int requiredPoints() {
        return base.requiredPoints();
    }

    public int requiredSpentPoints() {
        return base.requiredSpentPoints();
    }

    public int requiredExclusions() {
        return base.requiredExclusions();
    }

    public int pointsPerLevel() {
        return pointsPerLevel;
    }

    public void dispose(DisposeContext context) {
        base.dispose(context);
    }
}
