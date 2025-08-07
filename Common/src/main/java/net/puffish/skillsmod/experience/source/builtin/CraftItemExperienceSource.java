package net.puffish.skillsmod.experience.source.builtin;

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

public class CraftItemExperienceSource implements ExperienceSource {
	private static final Identifier ID = SkillsMod.createIdentifier("craft_item");
	private static final Prototype<Data> PROTOTYPE = Prototype.create(ID);

	static {
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_player"),
				BuiltinPrototypes.PLAYER,
				OperationFactory.create(Data::player)
		);
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_crafted_item_stack"),
				BuiltinPrototypes.ITEM_STACK,
				OperationFactory.create(Data::itemStack)
		);
	}

	private final Calculation<Data> calculation;

	private CraftItemExperienceSource(Calculation<Data> calculation) {
		this.calculation = calculation;
	}

	public static void register() {
		SkillsAPI.registerExperienceSource(
				ID,
				CraftItemExperienceSource::parse
		);
	}

	private static Result<CraftItemExperienceSource, Problem> parse(ExperienceSourceConfigContext context) {
		return context.getData().andThen(rootElement ->
				LegacyCalculation.parse(rootElement, PROTOTYPE, context)
						.mapSuccess(CraftItemExperienceSource::new)
		);
	}

	private record Data(ServerPlayerEntity player, ItemStack itemStack) { }

	public int getValue(ServerPlayerEntity player, ItemStack itemStack) {
		return (int) Math.round(calculation.evaluate(
				new Data(player, itemStack)
		));
	}

	@Override
	public void dispose(ExperienceSourceDisposeContext context) {
		// Nothing to do.
	}

}
