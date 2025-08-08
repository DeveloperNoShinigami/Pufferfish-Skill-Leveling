package net.puffish.skillsmod.experience.source.builtin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
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
import net.puffish.skillsmod.calculation.operation.builtin.StatCondition;

public class IncreaseStatExperienceSource implements ExperienceSource {
	private static final Identifier ID = SkillsMod.createIdentifier("increase_stat");
	private static final Prototype<Data> PROTOTYPE = Prototype.create(ID);

	static {
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_player"),
				BuiltinPrototypes.PLAYER,
				OperationFactory.create(Data::player)
		);
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_stat"),
				BuiltinPrototypes.STAT,
				OperationFactory.create(Data::stat)
		);
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_increase_amount"),
				BuiltinPrototypes.NUMBER,
				OperationFactory.create(data -> (double) data.amount())
		);
	}

	private final Calculation<Data> calculation;

	private IncreaseStatExperienceSource(Calculation<Data> calculation) {
		this.calculation = calculation;
	}

	public static void register() {
		SkillsAPI.registerExperienceSource(
				ID,
				IncreaseStatExperienceSource::parse
		);
	}

	private static Result<IncreaseStatExperienceSource, Problem> parse(ExperienceSourceConfigContext context) {
		return context.getData().andThen(rootElement ->
				LegacyCalculation.parse(rootElement, PROTOTYPE, context)
						.mapSuccess(IncreaseStatExperienceSource::new)
		);
	}

	private record Data(ServerPlayerEntity player, Stat<?> stat, int amount) { }

	public int getValue(ServerPlayerEntity player, Stat<?> stat, int amount) {
		return (int) Math.round(calculation.evaluate(
				new Data(player, stat, amount)
		));
	}

	@Override
	public void dispose(ExperienceSourceDisposeContext context) {
		// Nothing to do.
	}

}
