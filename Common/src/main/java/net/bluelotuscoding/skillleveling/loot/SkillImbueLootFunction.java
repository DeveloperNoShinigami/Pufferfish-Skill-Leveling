package net.bluelotuscoding.skillleveling.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.registry.ModLootFunctions;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.util.Identifier;

/**
 * A loot function that applies random skill imbuements to eligible equipment.
 */
public class SkillImbueLootFunction extends ConditionalLootFunction {
    public static final Identifier ID = SkillLevelingMod.createIdentifier("skill_imbue");

    public SkillImbueLootFunction(LootCondition[] conditions) {
        super(conditions);
    }

    @Override
    public ItemStack process(ItemStack stack, LootContext context) {
        Identifier lootTableId = null;
        var entity = context.get(net.minecraft.loot.context.LootContextParameters.THIS_ENTITY);
        if (entity != null) {
            Identifier entityId = net.minecraft.registry.Registries.ENTITY_TYPE.getId(entity.getType());
            lootTableId = new Identifier(entityId.getNamespace(), "entities/" + entityId.getPath());
        }

        SkillLevelingMod.getInstance().getLootImbueManager().applyRandomImbue(stack, context, lootTableId);
        return stack;
    }

    @Override
    public LootFunctionType getType() {
        return ModLootFunctions.SKILL_IMBUE;
    }

    public static ConditionalLootFunction.Builder<?> builder() {
        return builder(SkillImbueLootFunction::new);
    }

    public static class Serializer extends ConditionalLootFunction.Serializer<SkillImbueLootFunction> {
        @Override
        public SkillImbueLootFunction fromJson(JsonObject jsonObject,
                JsonDeserializationContext jsonDeserializationContext, LootCondition[] lootConditions) {
            return new SkillImbueLootFunction(lootConditions);
        }
    }
}
