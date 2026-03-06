package net.bluelotuscoding.skillleveling.forge.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * A Forge LootModifier that applies random skill imbuements to generated
 * equipment.
 */
public class SkillImbueLootModifier extends LootModifier {
    public static final Codec<SkillImbueLootModifier> CODEC = RecordCodecBuilder
            .create(inst -> codecStart(inst).apply(inst, SkillImbueLootModifier::new));

    public SkillImbueLootModifier(LootCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
            LootContext context) {
        net.minecraft.util.Identifier lootTableId = context.getQueriedLootTableId();
        if (lootTableId == null) {
            lootTableId = net.bluelotuscoding.skillleveling.loot.LootMatchingUtil.inferLootTableId(context);
        }

        for (ItemStack stack : generatedLoot) {
            if (!stack.isEmpty() && !net.bluelotuscoding.skillleveling.util.ImbuedSkillHelper.isImbued(stack)) {
                SkillLevelingMod.getInstance().getLootImbueManager().applyRandomImbue(stack, context, lootTableId);
            }
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
