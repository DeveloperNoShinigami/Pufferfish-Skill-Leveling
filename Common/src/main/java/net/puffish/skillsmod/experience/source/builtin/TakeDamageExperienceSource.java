package net.puffish.skillsmod.experience.source.builtin;

import net.minecraft.entity.damage.DamageSource;
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

import java.util.Optional;

public class TakeDamageExperienceSource implements ExperienceSource {
    private static final Identifier ID = SkillsMod.createIdentifier("take_damage");
    private static final Prototype<Data> PROTOTYPE = Prototype.create(ID);

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
                SkillsMod.createIdentifier("get_damage_source"),
                BuiltinPrototypes.DAMAGE_SOURCE,
                OperationFactory.create(Data::damageSource)
        );
        PROTOTYPE.registerOperation(
                SkillsMod.createIdentifier("get_taken_damage"),
                BuiltinPrototypes.NUMBER,
                OperationFactory.create(data -> (double) data.damage())
        );
    }

    private final Calculation<Data> calculation;

    private TakeDamageExperienceSource(Calculation<Data> calculation) {
        this.calculation = calculation;
    }

    public static void register() {
        SkillsAPI.registerExperienceSource(
                ID,
                TakeDamageExperienceSource::parse
        );
    }

    private static Result<TakeDamageExperienceSource, Problem> parse(ExperienceSourceConfigContext context) {
        return context.getData().andThen(rootElement ->
                LegacyCalculation.parse(rootElement, PROTOTYPE, context)
                        .mapSuccess(TakeDamageExperienceSource::new)
        );
    }

    private record Data(ServerPlayerEntity player, ItemStack weapon, float damage, DamageSource damageSource) { }

    public int getValue(ServerPlayerEntity player, ItemStack weapon, float damage, DamageSource damageSource) {
        return (int) Math.round(calculation.evaluate(
                new Data(player, weapon, damage, damageSource)
        ));
    }

    @Override
    public void dispose(ExperienceSourceDisposeContext context) {
        // Nothing to do.
    }

}
