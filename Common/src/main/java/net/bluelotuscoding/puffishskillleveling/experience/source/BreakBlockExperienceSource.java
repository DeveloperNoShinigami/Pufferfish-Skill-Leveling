package net.bluelotuscoding.puffishskillleveling.experience.source;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.calculation.Calculation;
import net.puffish.skillsmod.api.calculation.Variables;
import net.puffish.skillsmod.api.calculation.operation.OperationFactory;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.calculation.prototype.Prototype;
import net.puffish.skillsmod.api.experience.source.ExperienceSource;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceConfigContext;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceDisposeContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import java.util.ArrayList;
import java.util.Map;

/**
 * Grants experience when a player breaks a block with a tool.
 */
public class BreakBlockExperienceSource implements ExperienceSource {
    public static final ResourceLocation ID = SkillsMod.createIdentifier("break_block");
    private static final Prototype<Data> PROTOTYPE = Prototype.create(ID);

    static {
        PROTOTYPE.registerOperation(
                SkillsMod.createIdentifier("get_player"),
                BuiltinPrototypes.PLAYER,
                OperationFactory.create(Data::player)
        );
        PROTOTYPE.registerOperation(
                SkillsMod.createIdentifier("get_broken_block_state"),
                BuiltinPrototypes.BLOCK_STATE,
                OperationFactory.create(Data::blockState)
        );
        PROTOTYPE.registerOperation(
                SkillsMod.createIdentifier("get_tool_item_stack"),
                BuiltinPrototypes.ITEM_STACK,
                OperationFactory.create(Data::tool)
        );
    }

    private final Calculation<Data> calculation;

    private BreakBlockExperienceSource(Calculation<Data> calculation) {
        this.calculation = calculation;
    }

    public static void register() {
        SkillsAPI.registerExperienceSource(ID, BreakBlockExperienceSource::parse);
    }

    private static Result<BreakBlockExperienceSource, Problem> parse(ExperienceSourceConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(obj -> obj.noUnused(o -> parse(o, context)));
    }

    private static Result<BreakBlockExperienceSource, Problem> parse(JsonObject rootObject, ExperienceSourceConfigContext context) {
        var problems = new ArrayList<Problem>();

        var variables = rootObject.get("variables")
                .getSuccess()
                .flatMap(variablesElement -> Variables.parse(variablesElement, PROTOTYPE, context)
                        .ifFailure(problems::add)
                        .getSuccess())
                .orElseGet(() -> Variables.create(Map.of()));

        var optCalculation = rootObject.get("experience")
                .andThen(experienceElement -> Calculation.parse(
                        experienceElement,
                        variables,
                        context
                ))
                .ifFailure(problems::add)
                .getSuccess();

        if (problems.isEmpty()) {
            return Result.success(new BreakBlockExperienceSource(optCalculation.orElseThrow()));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    private record Data(ServerPlayer player, BlockState blockState, ItemStack tool) { }

    public int getValue(ServerPlayer player, BlockState blockState, ItemStack tool) {
        return (int) Math.round(calculation.evaluate(new Data(player, blockState, tool)));
    }

    @Override
    public void dispose(ExperienceSourceDisposeContext context) {
        // Nothing to dispose.
    }
}

