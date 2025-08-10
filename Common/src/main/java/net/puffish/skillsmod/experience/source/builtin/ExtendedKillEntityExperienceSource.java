package net.puffish.skillsmod.experience.source.builtin;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.calculation.Calculation;
import net.puffish.skillsmod.api.calculation.Variables;
import net.puffish.skillsmod.api.calculation.operation.OperationFactory;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.calculation.prototype.Prototype;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.calculation.LegacyBuiltinPrototypes;
import net.puffish.skillsmod.experience.source.builtin.util.AntiFarmingPerChunk;
import net.puffish.skillsmod.util.LegacyUtils;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.Optional;

public class ExtendedKillEntityExperienceSource extends KillEntityExperienceSource {
        protected static final Identifier ID = SkillsMod.createIdentifier("kill_entity");
        protected static final Prototype<Data> PROTOTYPE = Prototype.create(ID);

        static {
                PROTOTYPE.registerOperation(
                                SkillsMod.createIdentifier("get_player"),
                                BuiltinPrototypes.PLAYER,
                                OperationFactory.create(Data::player)
                );
                PROTOTYPE.registerOperation(
                                SkillsMod.createIdentifier("get_weapon_item_stack"),
                                BuiltinPrototypes.ITEM_STACK,
                                OperationFactory.create(Data::weapon)
                );
                PROTOTYPE.registerOperation(
                                SkillsMod.createIdentifier("get_killed_living_entity"),
                                BuiltinPrototypes.LIVING_ENTITY,
                                OperationFactory.create(Data::entity)
                );
                PROTOTYPE.registerOperation(
                                SkillsMod.createIdentifier("get_damage_source"),
                                BuiltinPrototypes.DAMAGE_SOURCE,
                                OperationFactory.create(Data::damageSource)
                );
                PROTOTYPE.registerOperation(
                                SkillsMod.createIdentifier("get_dropped_experience"),
                                BuiltinPrototypes.NUMBER,
                                OperationFactory.create(Data::entityDroppedXp)
                );
        }

        private ExtendedKillEntityExperienceSource(Calculation<Data> calculation, Optional<AntiFarmingPerChunk> optAntiFarming) {
                super(calculation, optAntiFarming);
        }

        public static void register() {
                SkillsAPI.registerExperienceSource(
                                ID,
                                ExtendedKillEntityExperienceSource::parse
                );
        }

        private static Result<ExtendedKillEntityExperienceSource, Problem> parse(ExperienceSourceConfigContext context) {
                return context.getData()
                                .andThen(JsonElement::getAsObject)
                                .andThen(LegacyUtils.wrapNoUnused(rootObject -> parse(rootObject, context), context));
        }
        private static Result<ExtendedKillEntityExperienceSource, Problem> parse(JsonObject rootObject, ConfigContext context) {
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

                var optAntiFarming = rootObject.get("anti_farming")
                                .getSuccess()
                                .flatMap(element -> AntiFarmingPerChunk.parse(element, context)
                                                .ifFailure(problems::add)
                                                .getSuccess()
                                                .flatMap(Function.identity())
                                );

                if (problems.isEmpty()) {
                        return Result.success(new ExtendedKillEntityExperienceSource(
                                        optCalculation.orElseThrow(),
                                        optAntiFarming
                        ));
                } else {
                        return Result.failure(Problem.combine(problems));
                }
        }
}