package net.puffish.skillsmod.experience.source.builtin;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.calculation.Calculation;
import net.puffish.skillsmod.api.calculation.Variables;
import net.puffish.skillsmod.api.calculation.operation.OperationFactory;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.calculation.prototype.Prototype;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import java.util.ArrayList;
import java.util.Map;

public class ExtendedCraftItemExperienceSource extends CraftItemExperienceSource {
        protected static final Identifier ID = SkillsMod.createIdentifier("craft_item");
        protected static final Prototype<Data> PROTOTYPE = Prototype.create(ID);

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

        private ExtendedCraftItemExperienceSource(Calculation<Data> calculation) {
                super(calculation);
        }

        public static void register() {
                SkillsAPI.registerExperienceSource(
                                ID,
                                ExtendedCraftItemExperienceSource::parse
                );
        }

        private static Result<ExtendedCraftItemExperienceSource, Problem> parse(ExperienceSourceConfigContext context) {
                return context.getData()
                                .andThen(JsonElement::getAsObject)
                                .andThen(rootObject -> rootObject.noUnused(o -> parse(o, context)));
        }

        private static Result<ExtendedCraftItemExperienceSource, Problem> parse(JsonObject rootObject, ExperienceSourceConfigContext context) {
                var problems = new ArrayList<Problem>();

                var variables = rootObject.get("variables")
                                .getSuccess() // ignore failure because this property is optional
                                .flatMap(variablesElement -> Variables.parse(variablesElement, PROTOTYPE, context)
                                                .ifFailure(problems::add)
                                                .getSuccess()
                                )
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
                        return Result.success(new ExtendedCraftItemExperienceSource(
                                        optCalculation.orElseThrow()
                        ));
                } else {
                        return Result.failure(Problem.combine(problems));
                }
        }
}
