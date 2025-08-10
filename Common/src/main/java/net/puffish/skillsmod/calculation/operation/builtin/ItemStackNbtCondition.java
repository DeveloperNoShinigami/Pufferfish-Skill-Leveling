package net.puffish.skillsmod.calculation.operation.builtin;

import net.minecraft.item.ItemStack;
import net.minecraft.predicate.NbtPredicate;
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

/**
 * Operation that tests an {@link ItemStack} against an {@link NbtPredicate}.
 */
public final class ItemStackNbtCondition implements Operation<ItemStack, Boolean> {
    private final NbtPredicate nbt;

    private ItemStackNbtCondition(NbtPredicate nbt) {
        this.nbt = nbt;
    }

    public static void register() {
        BuiltinPrototypes.ITEM_STACK.registerOperation(
                SkillsMod.createIdentifier("test_nbt"),
                BuiltinPrototypes.BOOLEAN,
                ItemStackNbtCondition::parse
        );
    }

    public static Result<ItemStackNbtCondition, Problem> parse(OperationConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(LegacyUtils.wrapNoUnused(ItemStackNbtCondition::parse, context));
    }

    public static Result<ItemStackNbtCondition, Problem> parse(JsonObject rootObject) {
        var problems = new ArrayList<Problem>();

        var optNbt = rootObject.get("nbt")
                .andThen(BuiltinJson::parseNbtPredicate)
                .ifFailure(problems::add)
                .getSuccess();

        if (optNbt.isEmpty()) {
            problems.add(rootObject.getPath().createProblem("Missing 'nbt' property."));
        }

        if (problems.isEmpty()) {
            return Result.success(new ItemStackNbtCondition(optNbt.orElseThrow()));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    @Override
    public Optional<Boolean> apply(ItemStack itemStack) {
        return Optional.of(nbt.test(itemStack));
    }
}

