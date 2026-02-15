package net.bluelotuscoding.skillleveling.forge.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.bluelotuscoding.skillleveling.loot.UnifiedLootConfig;
import net.bluelotuscoding.skillleveling.loot.LootMatchingUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Universal Loot Modifier — a standard Forge Global Loot Modifier that injects
 * items into chest loot tables based on the unified configuration structure.
 *
 * Entity drops are now handled by LootInjectionHandler for better persistence.
 */
public class UniversalLootModifier extends LootModifier {

        // ========== Codec Records (Mapping to UnifiedLootConfig) ==========

        public static final Codec<UnifiedLootConfig.SkillPoolEntry> SKILL_POOL_CODEC = RecordCodecBuilder
                        .create(inst -> inst.group(
                                        Codec.STRING.fieldOf("skill")
                                                        .forGetter(UnifiedLootConfig.SkillPoolEntry::skill),
                                        Codec.INT.fieldOf("weight").forGetter(UnifiedLootConfig.SkillPoolEntry::weight))
                                        .apply(inst, UnifiedLootConfig.SkillPoolEntry::new));

        public static final Codec<UnifiedLootConfig.LootEntry> LOOT_ENTRY_CODEC = RecordCodecBuilder.create(inst -> inst
                        .group(
                                        Codec.STRING.optionalFieldOf("type", "item")
                                                        .forGetter(UnifiedLootConfig.LootEntry::type),
                                        Codec.STRING.optionalFieldOf("name", "")
                                                        .forGetter(UnifiedLootConfig.LootEntry::name),
                                        Codec.FLOAT.optionalFieldOf("chance", 1.0f)
                                                        .forGetter(UnifiedLootConfig.LootEntry::chance),
                                        Codec.INT.optionalFieldOf("weight", 1)
                                                        .forGetter(UnifiedLootConfig.LootEntry::weight),
                                        Codec.INT.optionalFieldOf("min_level", 1)
                                                        .forGetter(UnifiedLootConfig.LootEntry::minLevel),
                                        Codec.INT.optionalFieldOf("max_level", 1)
                                                        .forGetter(UnifiedLootConfig.LootEntry::maxLevel),
                                        Codec.STRING.optionalFieldOf("nbt").forGetter(UnifiedLootConfig.LootEntry::nbt),
                                        Codec.STRING.optionalFieldOf("skill")
                                                        .forGetter(UnifiedLootConfig.LootEntry::skill),
                                        SKILL_POOL_CODEC.listOf().optionalFieldOf("skills", List.of())
                                                        .forGetter(UnifiedLootConfig.LootEntry::skills))
                        .apply(inst, UnifiedLootConfig.LootEntry::new));

        public static final Codec<UnifiedLootConfig.Rolls> ROLLS_CODEC = RecordCodecBuilder.create(inst -> inst.group(
                        Codec.INT.optionalFieldOf("min", 1).forGetter(UnifiedLootConfig.Rolls::min),
                        Codec.INT.optionalFieldOf("max", 1).forGetter(UnifiedLootConfig.Rolls::max))
                        .apply(inst, UnifiedLootConfig.Rolls::new));

        public static final Codec<UnifiedLootConfig.LootGroup> LOOT_GROUP_CODEC = RecordCodecBuilder.create(inst -> inst
                        .group(
                                        Codec.STRING.listOf().fieldOf("targets")
                                                        .forGetter(UnifiedLootConfig.LootGroup::targets),
                                        Codec.FLOAT.optionalFieldOf("chance", 1.0f)
                                                        .forGetter(UnifiedLootConfig.LootGroup::chance),
                                        ROLLS_CODEC.optionalFieldOf("rolls", new UnifiedLootConfig.Rolls(1, 1))
                                                        .forGetter(UnifiedLootConfig.LootGroup::rolls),
                                        LOOT_ENTRY_CODEC.listOf().fieldOf("entries")
                                                        .forGetter(UnifiedLootConfig.LootGroup::entries))
                        .apply(inst, UnifiedLootConfig.LootGroup::new));

        // ========== Main Codec ==========

        public static final Codec<UniversalLootModifier> CODEC = RecordCodecBuilder.create(inst -> codecStart(inst)
                        .and(inst.group(
                                        LOOT_GROUP_CODEC.listOf().optionalFieldOf("chest_injection_groups", List.of())
                                                        .forGetter(m -> m.chestInjectionGroups),
                                        LOOT_GROUP_CODEC.listOf().optionalFieldOf("entity_drop_groups", List.of())
                                                        .forGetter(m -> m.entityDropGroups)))
                        .apply(inst, UniversalLootModifier::new));

        // ========== Instance Fields ==========

        private final List<UnifiedLootConfig.LootGroup> chestInjectionGroups;
        private final List<UnifiedLootConfig.LootGroup> entityDropGroups;

        public UniversalLootModifier(LootCondition[] conditionsIn,
                        List<UnifiedLootConfig.LootGroup> chestInjectionGroups,
                        List<UnifiedLootConfig.LootGroup> entityDropGroups) {
                super(conditionsIn);
                this.chestInjectionGroups = chestInjectionGroups;
                this.entityDropGroups = entityDropGroups;

                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                                .debug("[UniversalLoot] Constructor called. Groups: " + chestInjectionGroups.size()
                                                + " chest, "
                                                + entityDropGroups.size() + " entity.");
        }

        // ========== Core Logic ==========

        @Override
        protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
                        LootContext context) {
                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                                .debug("[UniversalLoot] doApply triggered. Queried ID: "
                                                + context.getQueriedLootTableId());

                Identifier lootTableId = context.getQueriedLootTableId();
                if (lootTableId == null) {
                        lootTableId = LootMatchingUtil.inferLootTableId(context);
                }

                if (lootTableId == null) {
                        net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                                        .debug("[UniversalLoot] Could not resolve Loot Table ID. Skipping.");
                        return generatedLoot;
                }

                String id = lootTableId.toString();
                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                                .debug("[UniversalLoot] Processing loot table: " + id);
                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                                .debug("[UniversalLoot] doApply entered for: " + id);

                // Check chest injection groups
                for (UnifiedLootConfig.LootGroup group : chestInjectionGroups) {
                        if (LootMatchingUtil.matchesTarget(id, group.targets())) {
                                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                                                .debug("[UniversalLoot] Injecting (Chest) into: " + id);
                                injectItems(generatedLoot, group, context);
                        }
                }

                // Check entity drop groups
                for (UnifiedLootConfig.LootGroup group : entityDropGroups) {
                        if (LootMatchingUtil.matchesTarget(id, group.targets())) {
                                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                                                .debug("[UniversalLoot] Injecting (Entity) into: " + id);
                                injectItems(generatedLoot, group, context);
                        }
                }

                return generatedLoot;
        }

        private void injectItems(ObjectArrayList<ItemStack> generatedLoot, UnifiedLootConfig.LootGroup group,
                        LootContext context) {
                var random = context.getRandom();

                if (group.chance() < 1.0f && random.nextFloat() >= group.chance()) {
                        return;
                }

                int rolls = group.rolls().min();
                if (group.rolls().max() > group.rolls().min()) {
                        rolls += random.nextInt(group.rolls().max() - group.rolls().min() + 1);
                }

                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                                .debug("[UniversalLoot] Rolling " + rolls + " times.");

                List<UnifiedLootConfig.LootEntry> validEntries = new ArrayList<>();
                int totalWeight = 0;
                for (UnifiedLootConfig.LootEntry entry : group.entries()) {
                        if (entry.weight() > 0) {
                                validEntries.add(entry);
                                totalWeight += entry.weight();
                        }
                }

                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger().debug(
                                "[UniversalLoot] Found " + validEntries.size() + " valid entries. Total weight: "
                                                + totalWeight);

                if (totalWeight <= 0) {
                        return;
                }

                for (int i = 0; i < rolls; i++) {
                        int r = random.nextInt(totalWeight);
                        int current = 0;
                        for (UnifiedLootConfig.LootEntry entry : validEntries) {
                                current += entry.weight();
                                if (r < current) {
                                        processEntry(generatedLoot, entry, context);
                                        break;
                                }
                        }
                }
        }

        private void processEntry(ObjectArrayList<ItemStack> generatedLoot, UnifiedLootConfig.LootEntry entry,
                        LootContext context) {
                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                                .debug("[UniversalLoot] Processing entry: " + entry.type() + " (" + entry.name()
                                                + ") - Chance: "
                                                + entry.chance());
                if (entry.chance() < 1.0f && context.getRandom().nextFloat() >= entry.chance()) {

                        net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                                        .debug("[UniversalLoot]   -> Chance failed.");
                        return;
                }

                ItemStack stack = net.bluelotuscoding.skillleveling.loot.LootStackFactory.createStack(entry,
                                context.getRandom());
                if (!stack.isEmpty()) {
                        net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLootImbueManager()
                                        .applyRandomImbue(
                                                        stack, context, context.getQueriedLootTableId());
                        generatedLoot.add(stack);
                }
        }

        @Override
        public Codec<? extends IGlobalLootModifier> codec() {
                return CODEC;
        }
}
