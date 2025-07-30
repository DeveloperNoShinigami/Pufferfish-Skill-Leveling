package net.puffish.skill_leveling.calculation.operation.builtin;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntryList;
import net.puffish.skill_leveling.SkillsMod;
import net.puffish.skill_leveling.api.calculation.operation.Operation;
import net.puffish.skill_leveling.api.calculation.operation.OperationConfigContext;
import net.puffish.skill_leveling.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skill_leveling.api.json.BuiltinJson;
import net.puffish.skill_leveling.api.json.JsonElement;
import net.puffish.skill_leveling.api.json.JsonObject;
import net.puffish.skill_leveling.api.util.Problem;
import net.puffish.skill_leveling.api.util.Result;
import net.puffish.skill_leveling.util.LegacyUtils;

import java.util.ArrayList;
import java.util.Optional;

public final class BlockCondition implements Operation<Block, Boolean> {
	private final RegistryEntryList<Block> blockEntries;

	private BlockCondition(RegistryEntryList<Block> blockEntries) {
		this.blockEntries = blockEntries;
	}

	public static void register() {
		BuiltinPrototypes.BLOCK.registerOperation(
				SkillsMod.createIdentifier("test"),
				BuiltinPrototypes.BOOLEAN,
				BlockCondition::parse
		);
	}

	public static Result<BlockCondition, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(LegacyUtils.wrapNoUnused(BlockCondition::parse, context));
	}

	public static Result<BlockCondition, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		var optBlock = rootObject.get("block")
				.andThen(BuiltinJson::parseBlockOrBlockTag)
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new BlockCondition(
					optBlock.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<Boolean> apply(Block block) {
		return Optional.of(blockEntries.contains(Registries.BLOCK.getEntry(block)));
	}
}
