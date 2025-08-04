package net.bluelotuscoding.puffishskillleveling.experience.source;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.calculation.Calculation;
import net.puffish.skillsmod.api.calculation.operation.OperationFactory;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.calculation.prototype.Prototype;
import net.puffish.skillsmod.api.experience.source.ExperienceSource;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceConfigContext;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceDisposeContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.calculation.LegacyBuiltinPrototypes;
import net.puffish.skillsmod.calculation.LegacyCalculation;
import net.puffish.skillsmod.calculation.operation.LegacyOperationRegistry;
import net.puffish.skillsmod.calculation.operation.builtin.AttributeOperation;
import net.puffish.skillsmod.calculation.operation.builtin.BlockStateCondition;
import net.puffish.skillsmod.calculation.operation.builtin.EffectOperation;
import net.puffish.skillsmod.calculation.operation.builtin.ItemStackCondition;
import net.puffish.skillsmod.calculation.operation.builtin.legacy.LegacyBlockTagCondition;
import net.puffish.skillsmod.calculation.operation.builtin.legacy.LegacyItemTagCondition;

/**
 * Grants experience for mining blocks.
 */
public class MineBlockExperienceSource implements ExperienceSource {
    public static final ResourceLocation ID = SkillsMod.createIdentifier("mine_block");
    private static final Prototype<Data> PROTOTYPE = Prototype.create(ID);

    static {
        PROTOTYPE.registerOperation(
                SkillsMod.createIdentifier("get_player"),
                BuiltinPrototypes.PLAYER,
                OperationFactory.create(Data::player)
        );
        PROTOTYPE.registerOperation(
                SkillsMod.createIdentifier("get_mined_block_state"),
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

    private MineBlockExperienceSource(Calculation<Data> calculation) {
        this.calculation = calculation;
    }

    public static void register() {
        SkillsAPI.registerExperienceSource(ID, MineBlockExperienceSource::parse);
    }

    private static Result<MineBlockExperienceSource, Problem> parse(ExperienceSourceConfigContext context) {
        return context.getData().andThen(rootElement ->
                LegacyCalculation.parse(rootElement, PROTOTYPE, context)
                        .mapSuccess(MineBlockExperienceSource::new)
        );
    }

    private record Data(ServerPlayer player, BlockState blockState, ItemStack tool) { }

    public int getValue(ServerPlayer player, BlockState blockState, ItemStack tool) {
        return (int) Math.round(calculation.evaluate(new Data(player, blockState, tool)));
    }

    @Override
    public void dispose(ExperienceSourceDisposeContext context) {
        // Nothing to do.
    }

    static {
        var legacy = new LegacyOperationRegistry<>(PROTOTYPE);
        legacy.registerBooleanFunction(
                "block",
                BlockStateCondition::parse,
                Data::blockState
        );
        legacy.registerBooleanFunction(
                "block_state",
                BlockStateCondition::parse,
                Data::blockState
        );
        legacy.registerBooleanFunction(
                "block_tag",
                LegacyBlockTagCondition::parse,
                Data::blockState
        );
        legacy.registerBooleanFunction(
                "tool",
                ItemStackCondition::parse,
                Data::tool
        );
        legacy.registerBooleanFunction(
                "tool_nbt",
                ItemStackCondition::parse,
                Data::tool
        );
        legacy.registerBooleanFunction(
                "tool_tag",
                LegacyItemTagCondition::parse,
                Data::tool
        );
        legacy.registerNumberFunction(
                "player_effect",
                effect -> (double) (effect.getAmplifier() + 1),
                EffectOperation::parse,
                Data::player
        );
        legacy.registerNumberFunction(
                "player_attribute",
                net.minecraft.world.entity.ai.attributes.AttributeInstance::getValue,
                AttributeOperation::parse,
                Data::player
        );

        LegacyBuiltinPrototypes.registerAlias(
                PROTOTYPE,
                SkillsMod.createIdentifier("player"),
                SkillsMod.createIdentifier("get_player")
        );
        LegacyBuiltinPrototypes.registerAlias(
                PROTOTYPE,
                SkillsMod.createIdentifier("mined_block_state"),
                SkillsMod.createIdentifier("get_mined_block_state")
        );
        LegacyBuiltinPrototypes.registerAlias(
                PROTOTYPE,
                SkillsMod.createIdentifier("tool_item_stack"),
                SkillsMod.createIdentifier("get_tool_item_stack")
        );
    }
}

