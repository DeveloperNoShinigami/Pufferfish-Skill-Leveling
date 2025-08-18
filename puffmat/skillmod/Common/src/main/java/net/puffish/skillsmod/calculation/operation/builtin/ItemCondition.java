package net.puffish.skillsmod.calculation.operation.builtin;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntryList;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.calculation.operation.Operation;
import net.puffish.skillsmod.api.calculation.operation.OperationConfigContext;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.json.BuiltinJson;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.util.LegacyUtils;

import java.util.ArrayList;
import java.util.Optional;

public final class ItemCondition implements Operation<Item, Boolean> {
	private final RegistryEntryList<Item> itemEntries;

	private ItemCondition(RegistryEntryList<Item> itemEntries) {
		this.itemEntries = itemEntries;
	}

	public static void register() {
		BuiltinPrototypes.ITEM.registerOperation(
				SkillsMod.createIdentifier("test"),
				BuiltinPrototypes.BOOLEAN,
				ItemCondition::parse
		);
	}

	public static Result<ItemCondition, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(LegacyUtils.wrapNoUnused(ItemCondition::parse, context));
	}

	public static Result<ItemCondition, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		var optItem = rootObject.get("item")
				.andThen(BuiltinJson::parseItemOrItemTag)
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new ItemCondition(
					optItem.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<Boolean> apply(Item item) {
		return Optional.of(itemEntries.contains(Registries.ITEM.getEntry(item)));
	}
}
