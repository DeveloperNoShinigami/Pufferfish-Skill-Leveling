package net.bluelotuscoding.skillleveling.loot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Common handler for Universal Loot Injection.
 * Used by Fabric to load and apply loot injections (parity with Forge GLMs).
 */
public class UniversalLootHandler extends JsonDataLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final List<UnifiedLootConfig.LootGroup> entityDropGroups = new ArrayList<>();
    private final List<UnifiedLootConfig.LootGroup> chestInjectionGroups = new ArrayList<>();

    public UniversalLootHandler() {
        super(GSON, "loot_modifiers");
    }

    public List<UnifiedLootConfig.LootGroup> getEntityDropGroups() {
        return entityDropGroups;
    }

    public List<UnifiedLootConfig.LootGroup> getChestInjectionGroups() {
        return chestInjectionGroups;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        entityDropGroups.clear();
        chestInjectionGroups.clear();

        for (var entry : prepared.entrySet()) {
            try {
                JsonElement element = entry.getValue();
                if (element.isJsonObject()) {
                    JsonObject json = element.getAsJsonObject();
                    if (json.has("type")
                            && json.get("type").getAsString().equals("puffish_skill_leveling:universal_loot")) {
                        parseUniversalLoot(json);
                    }
                }
            } catch (Exception e) {
                SkillLevelingMod.getInstance().getLogger()
                        .error("Error parsing universal loot " + entry.getKey() + ": " + e.getMessage());
            }
        }

        SkillLevelingMod.getInstance().getLogger().info("[UniversalLootHandler] Loaded: " + entityDropGroups.size()
                + " entity groups, " + chestInjectionGroups.size() + " chest groups. Source files: " + prepared.size());
        if (prepared.isEmpty()) {
            SkillLevelingMod.getInstance().getLogger()
                    .warn("[UniversalLootHandler] No JSON files found in data/*/loot_modifiers/");
        }
    }

    private void parseUniversalLoot(JsonObject json) {
        try {
            if (json.has("entity_drop_groups")) {
                for (var e : json.getAsJsonArray("entity_drop_groups")) {
                    entityDropGroups.add(parseGroup(e.getAsJsonObject()));
                }
            }
            if (json.has("chest_injection_groups")) {
                for (var e : json.getAsJsonArray("chest_injection_groups")) {
                    chestInjectionGroups.add(parseGroup(e.getAsJsonObject()));
                }
            }
        } catch (Exception e) {
            SkillLevelingMod.getInstance().getLogger()
                    .error("Error parsing universal loot structure: " + e.getMessage());
        }
    }

    private UnifiedLootConfig.LootGroup parseGroup(JsonObject obj) {
        List<String> targets = new ArrayList<>();
        obj.getAsJsonArray("targets").forEach(t -> targets.add(t.getAsString()));

        float chance = obj.has("chance") ? obj.get("chance").getAsFloat() : 1.0f;

        UnifiedLootConfig.Rolls rolls = new UnifiedLootConfig.Rolls(1, 1);
        if (obj.has("rolls")) {
            JsonObject r = obj.getAsJsonObject("rolls");
            rolls = new UnifiedLootConfig.Rolls(r.get("min").getAsInt(), r.get("max").getAsInt());
        }

        List<UnifiedLootConfig.LootEntry> entries = new ArrayList<>();
        for (var e : obj.getAsJsonArray("entries")) {
            entries.add(parseEntry(e.getAsJsonObject()));
        }

        return new UnifiedLootConfig.LootGroup(targets, chance, rolls, entries);
    }

    private UnifiedLootConfig.LootEntry parseEntry(JsonObject obj) {
        String type = obj.has("type") ? obj.get("type").getAsString() : "item";
        String name = obj.has("name") ? obj.get("name").getAsString() : "";
        float chance = obj.has("chance") ? obj.get("chance").getAsFloat() : 1.0f;
        int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 1;
        int minLevel = obj.has("min_level") ? obj.get("min_level").getAsInt() : 1;
        int maxLevel = obj.has("max_level") ? obj.get("max_level").getAsInt() : 1;

        java.util.Optional<String> nbt = obj.has("nbt") ? java.util.Optional.of(obj.get("nbt").getAsString())
                : java.util.Optional.empty();
        java.util.Optional<String> skill = obj.has("skill") ? java.util.Optional.of(obj.get("skill").getAsString())
                : java.util.Optional.empty();

        List<UnifiedLootConfig.SkillPoolEntry> skills = new ArrayList<>();
        if (obj.has("skills")) {
            for (var s : obj.getAsJsonArray("skills")) {
                JsonObject so = s.getAsJsonObject();
                skills.add(new UnifiedLootConfig.SkillPoolEntry(so.get("skill").getAsString(),
                        so.get("weight").getAsInt()));
            }
        }

        return new UnifiedLootConfig.LootEntry(type, name, chance, weight, minLevel, maxLevel, nbt, skill, skills);
    }

    public void injectLoot(Identifier lootTableId, net.minecraft.loot.LootTable.Builder builder) {
        if (lootTableId == null)
            return;
        String id = lootTableId.toString();

        for (UnifiedLootConfig.LootGroup group : chestInjectionGroups) {
            if (LootMatchingUtil.matchesTarget(id, group.targets())) {
                injectIntoPool(builder, group);
            }
        }

        for (UnifiedLootConfig.LootGroup group : entityDropGroups) {
            if (LootMatchingUtil.matchesTarget(id, group.targets())) {
                injectIntoPool(builder, group);
            }
        }
    }

    private void injectIntoPool(net.minecraft.loot.LootTable.Builder builder, UnifiedLootConfig.LootGroup group) {
        // Create a new pool for this group
        var poolBuilder = net.minecraft.loot.LootPool.builder()
                .rolls(net.minecraft.loot.provider.number.UniformLootNumberProvider.create(group.rolls().min(),
                        group.rolls().max()));

        if (group.chance() < 1.0f) {
            poolBuilder.conditionally(net.minecraft.loot.condition.RandomChanceLootCondition.builder(group.chance()));
        }

        for (UnifiedLootConfig.LootEntry entry : group.entries()) {
            if (entry.weight() <= 0)
                continue;

            // In Fabric, we add entries to the pool.
            // For Skill Tomes/Charms, we can add the item and then apply a function
            // to set the NBT/Skill data, similar to how SkillImbueLootFunction works.

            // To keep it simple and consistent with our StackFactory, we'll use a
            // 'LootItem' entry if it's a standard item, and for special types we'll
            // need to apply the right functions.

            if ("item".equals(entry.type()) || "skill_tome".equals(entry.type())
                    || "skill_charm".equals(entry.type())) {
                Identifier itemId = new Identifier(entry.name());
                var item = net.minecraft.registry.Registries.ITEM.get(itemId);
                if (item != net.minecraft.item.Items.AIR) {
                    var itemEntry = net.minecraft.loot.entry.ItemEntry
                            .builder(item)
                            .weight(entry.weight());

                    if (item instanceof net.bluelotuscoding.skillleveling.item.SkillTomeItem) {
                        itemEntry.apply(net.bluelotuscoding.skillleveling.loot.RandomizeSkillTomeLootFunction.builder(
                                entry.minLevel(), entry.maxLevel())
                                .withSkill(entry.skill().orElse(null))
                                .withSkills(entry.skills()));
                    }

                    poolBuilder.with(itemEntry);
                }
            }
        }

        builder.pool(poolBuilder);
    }

    public void injectLoot(List<ItemStack> generatedLoot, LootContext context, @Nullable Identifier lootTableId) {
        if (lootTableId == null) {
            return;
        }
        String id = lootTableId.toString();

        // Check chest injection groups
        for (UnifiedLootConfig.LootGroup group : chestInjectionGroups) {
            if (LootMatchingUtil.matchesTarget(id, group.targets())) {
                applyGroup(generatedLoot, group, context.getWorld(),
                        context.get(LootContextParameters.ORIGIN),
                        context.get(LootContextParameters.THIS_ENTITY),
                        context.getRandom(), lootTableId);
            }
        }

        // Check entity drop groups
        for (UnifiedLootConfig.LootGroup group : entityDropGroups) {
            if (LootMatchingUtil.matchesTarget(id, group.targets())) {
                applyGroup(generatedLoot, group, context.getWorld(),
                        context.get(LootContextParameters.ORIGIN),
                        context.get(LootContextParameters.THIS_ENTITY),
                        context.getRandom(), lootTableId);
            }
        }
    }

    /**
     * Raw overload for when LootContext is not available.
     */
    public void injectLoot(List<ItemStack> generatedLoot, net.minecraft.entity.Entity entity,
            @Nullable Identifier lootTableId) {
        if (lootTableId == null) {
            return;
        }
        String id = lootTableId.toString();

        for (UnifiedLootConfig.LootGroup group : entityDropGroups) {
            if (LootMatchingUtil.matchesTarget(id, group.targets())) {
                applyGroup(generatedLoot, group, entity.getWorld(), entity.getPos(), entity,
                        entity.getWorld().getRandom(), lootTableId);
            }
        }
    }

    private void applyGroup(List<ItemStack> generatedLoot, UnifiedLootConfig.LootGroup group,
            net.minecraft.world.World world, @Nullable net.minecraft.util.math.Vec3d origin,
            @Nullable net.minecraft.entity.Entity entity, Random random, @Nullable Identifier lootTableId) {
        if (group.chance() < 1.0f && random.nextFloat() >= group.chance()) {
            return;
        }

        int rolls = group.rolls().min();
        if (group.rolls().max() > group.rolls().min()) {
            rolls += random.nextInt(group.rolls().max() - group.rolls().min() + 1);
        }

        List<UnifiedLootConfig.LootEntry> validEntries = new ArrayList<>();
        int totalWeight = 0;
        for (UnifiedLootConfig.LootEntry entry : group.entries()) {
            if (entry.weight() > 0) {
                validEntries.add(entry);
                totalWeight += entry.weight();
            }
        }

        if (validEntries.isEmpty() || totalWeight <= 0)
            return;

        for (int i = 0; i < rolls; i++) {
            int r = random.nextInt(totalWeight);
            int current = 0;
            for (UnifiedLootConfig.LootEntry entry : validEntries) {
                current += entry.weight();
                if (r < current) {
                    processEntry(generatedLoot, entry, world, origin, entity, random, lootTableId);
                    break;
                }
            }
        }
    }

    private void processEntry(List<ItemStack> generatedLoot, UnifiedLootConfig.LootEntry entry,
            net.minecraft.world.World world, @Nullable net.minecraft.util.math.Vec3d origin,
            @Nullable net.minecraft.entity.Entity entity, Random random, @Nullable Identifier lootTableId) {
        if (entry.chance() < 1.0f && random.nextFloat() >= entry.chance()) {
            return;
        }

        ItemStack stack = LootStackFactory.createStack(entry, random);
        if (!stack.isEmpty()) {
            // CRITICAL: Trigger imbuement for our custom injected items.
            SkillLevelingMod.getInstance().getLootImbueManager().applyRandomImbue(stack, world, origin, entity, random,
                    lootTableId);
            generatedLoot.add(stack);
        }
    }

}
