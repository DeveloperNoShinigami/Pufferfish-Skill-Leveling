package net.puffish.skillsmod.experience.source.builtin;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
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
import net.puffish.skillsmod.calculation.LegacyCalculation;

public class MineBlockExperienceSource implements ExperienceSource {
	private static final Identifier ID = SkillsMod.createIdentifier("mine_block");
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
		SkillsAPI.registerExperienceSource(
				ID,
				MineBlockExperienceSource::parse
		);
	}

	private static Result<MineBlockExperienceSource, Problem> parse(ExperienceSourceConfigContext context) {
		return context.getData().andThen(rootElement ->
				LegacyCalculation.parse(rootElement, PROTOTYPE, context)
						.mapSuccess(MineBlockExperienceSource::new)
		);
	}

	private record Data(ServerPlayerEntity player, BlockState blockState, ItemStack tool) { }

	public int getValue(ServerPlayerEntity player, BlockState blockState, ItemStack tool) {
		return (int) Math.round(calculation.evaluate(
				new Data(player, blockState, tool)
		));
	}

	@Override
	public void dispose(ExperienceSourceDisposeContext context) {
		// Nothing to do.
	}

}
