package net.bluelotuscoding.puffishskillleveling.calculation.operation.builtin;

import net.minecraft.item.ItemStack;
import net.minecraft.predicate.NbtPredicate;
import net.bluelotuscoding.puffishskillleveling.SkillsMod;
import net.bluelotuscoding.puffishskillleveling.api.calculation.operation.Operation;
import net.bluelotuscoding.puffishskillleveling.api.calculation.operation.OperationConfigContext;
import net.bluelotuscoding.puffishskillleveling.api.calculation.prototype.BuiltinPrototypes;
import net.bluelotuscoding.puffishskillleveling.api.json.BuiltinJson;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonElement;
import net.bluelotuscoding.puffishskillleveling.api.json.JsonObject;
import net.bluelotuscoding.puffishskillleveling.api.util.Problem;
import net.bluelotuscoding.puffishskillleveling.api.util.Result;
import net.bluelotuscoding.puffishskillleveling.util.LegacyUtils;

import java.util.ArrayList;
import java.util.Optional;

public final class ItemStackNbtCondition implements Operation<ItemStack, Boolean> {
       private final NbtPredicate nbt;

       private ItemStackNbtCondition(NbtPredicate nbt) {
               this.nbt = nbt;
       }

       public static void register() {
               BuiltinPrototypes.ITEM_STACK.registerOperation(
                               SkillsMod.createIdentifier("matches_nbt"),
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

               var nbt = rootObject.get("nbt")
                               .andThen(BuiltinJson::parseNbtPredicate)
                               .ifFailure(problems::add)
                               .getSuccess();

               if (problems.isEmpty()) {
                       return Result.success(new ItemStackNbtCondition(
                                       nbt.orElseThrow()
                       ));
               } else {
                       return Result.failure(Problem.combine(problems));
               }
       }

       @Override
       public Optional<Boolean> apply(ItemStack itemStack) {
               return Optional.of(nbt.test(itemStack));
       }
}
