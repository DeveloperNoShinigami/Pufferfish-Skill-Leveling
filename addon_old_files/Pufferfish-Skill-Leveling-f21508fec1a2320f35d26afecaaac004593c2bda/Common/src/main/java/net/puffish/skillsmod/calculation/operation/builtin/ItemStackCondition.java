package net.puffish.skillsmod.calculation.operation.builtin;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.NbtPredicate;
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

public final class ItemStackCondition implements Operation<ItemStack, Boolean> {
	private final Optional<RegistryEntryList<Item>> optItemEntries;
	private final Optional<NbtPredicate> optNbt;

	private ItemStackCondition(Optional<RegistryEntryList<Item>> optItemEntries, Optional<NbtPredicate> optNbt) {
		this.optItemEntries = optItemEntries;
		this.optNbt = optNbt;
	}

	public static void register() {
		BuiltinPrototypes.ITEM_STACK.registerOperation(
				SkillsMod.createIdentifier("test"),
				BuiltinPrototypes.BOOLEAN,
				ItemStackCondition::parse
		);
	}

	public static Result<ItemStackCondition, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(LegacyUtils.wrapNoUnused(ItemStackCondition::parse, context));
	}

	public static Result<ItemStackCondition, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		var optItem = rootObject.get("item")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(itemElement -> BuiltinJson.parseItemOrItemTag(itemElement)
						.ifFailure(problems::add)
						.getSuccess()
				);

		var optNbt = rootObject.get("nbt")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(stateElement -> BuiltinJson.parseNbtPredicate(stateElement)
						.ifFailure(problems::add)
						.getSuccess()
				);

		if (problems.isEmpty()) {
			return Result.success(new ItemStackCondition(
					optItem,
					optNbt
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<Boolean> apply(ItemStack itemStack) {
		return Optional.of(
				optItemEntries.map(itemEntries -> itemEntries.contains(itemStack.getRegistryEntry())).orElse(true)
						&& optNbt.map(nbt -> nbt.test(itemStack)).orElse(true)
		);
	}
}
