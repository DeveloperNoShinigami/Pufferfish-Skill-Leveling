package net.bluelotuscoding.skillleveling.util;

import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
import net.bluelotuscoding.skillleveling.data.GlobalLootConfig;
import net.bluelotuscoding.skillleveling.item.SkillTomeItem;
import net.bluelotuscoding.skillleveling.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.Identifier;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.ItemEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LootHelper {

    /**
     * Determine what items should be dropped when an entity is killed.
     */
    public static List<ItemStack> getDropsForEntity(Entity entity, Random random) {
        List<ItemStack> drops = new ArrayList<>();
        String entityId = EntityType.getId(entity.getType()).toString();
        var config = net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getGlobalLootConfigLoader()
                .getConfig();

        for (var group : config.entityDrops) {
            if (group.entityTypes.contains(entityId)) {
                for (var entry : group.entries) {
                    if (entry.chance > 0 && random.nextFloat() < entry.chance) {
                        ItemStack stack = createStackFromEntry(entry, random);
                        if (!stack.isEmpty()) {
                            drops.add(stack);
                        }
                    }
                }
            }
        }

        return drops;
    }

    private static ItemStack createStackFromEntry(GlobalLootConfig.LootEntry entry, Random random) {
        if ("skill_tome".equals(entry.type)) {
            return createRandomSkillTome(entry.minLevel, entry.maxLevel, random);
        } else {
            Identifier itemId = new Identifier(entry.item);
            var optionalItem = net.minecraft.registry.Registries.ITEM.getOrEmpty(itemId);
            if (optionalItem.isPresent()) {
                ItemStack stack = new ItemStack(optionalItem.get());
                if (entry.nbt != null && !entry.nbt.isEmpty()) {
                    try {
                        stack.setNbt(net.minecraft.nbt.StringNbtReader.parse(entry.nbt));
                    } catch (Exception e) {
                        net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                                .error("Error parsing NBT for loot entry " + entry.item + ": " + e.getMessage());
                    }
                }
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack createRandomSkillTome(int minLevel, int maxLevel, Random random) {
        var entries = new ArrayList<>(LeveledConfigStorage.getAllEntries().entrySet());
        if (entries.isEmpty())
            return ItemStack.EMPTY;

        Collections.shuffle(entries);
        for (var entry : entries) {
            var config = entry.getValue();
            // Any skill with a defined loot mode is "lootable" via tomes
            if (config.lootMode != null && config.categoryId != null) {
                int level = minLevel + random.nextInt(maxLevel - minLevel + 1);
                level = Math.max(1, Math.min(level, config.maxLevels));
                return SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, config.categoryId, entry.getKey(),
                        config.lootMode, level);
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Logic for injecting into vanilla chest loot tables.
     */
    public static void injectChestLoot(Identifier id, LootPool.Builder builder) {
        String lootTableId = id.toString();
        var config = net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getGlobalLootConfigLoader()
                .getConfig();

        for (var group : config.chestInjections) {
            boolean match = false;
            for (String target : group.containers) {
                if (lootTableId.contains(target)) {
                    match = true;
                    break;
                }
            }

            if (match) {
                for (var entry : group.entries) {
                    if (entry.weight > 0) {
                        if ("skill_tome".equals(entry.type)) {
                            // Since we can't easily generate a random skill tome item inside a static
                            // Builder during bootstrap,
                            // we fallback to common items or we'd need a custom LootPoolEntry for true
                            // randomness.
                            // For now, let's add a placeholder or a very basic version if it's too complex
                            // to do here.
                            // IDEALLY: Use a custom LootFunction.

                            // For Chest Injections, we'll use a trick: Inject the Skill Tome with a special
                            // "randomize" function.
                            // Actually, let's keep it simple for now as per user request: "jsut use our
                            // items as a default".
                            // If they want skill tomes in chests, we might need that custom function.

                            builder.with(ItemEntry.builder(ModItems.SKILL_TOME)
                                    .weight(entry.weight)
                                    .apply(net.bluelotuscoding.skillleveling.loot.RandomizeSkillTomeLootFunction
                                            .builder(entry.minLevel, entry.maxLevel)));
                        } else {
                            Identifier itemId = new Identifier(entry.item);
                            var item = net.minecraft.registry.Registries.ITEM.get(itemId);
                            if (item != null) {
                                var itemEntry = ItemEntry.builder(item).weight(entry.weight);
                                if (entry.nbt != null && !entry.nbt.isEmpty()) {
                                    try {
                                        itemEntry.apply(net.minecraft.loot.function.SetNbtLootFunction.builder(
                                                net.minecraft.nbt.StringNbtReader.parse(entry.nbt)));
                                    } catch (Exception ignored) {
                                    }
                                }
                                builder.with(itemEntry);
                            }
                        }
                    }
                }
            }
        }
    }
}
