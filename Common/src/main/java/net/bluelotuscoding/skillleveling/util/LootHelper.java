package net.bluelotuscoding.skillleveling.util;

import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
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
        EntityType<?> type = entity.getType();

        // 1. Check for basic mobs (Zombies, Skeletons, Pillagers, Enderman)
        if (isBasicMob(type)) {
            // Rare chance for Blank Tome (2%)
            if (random.nextFloat() < 0.02f) {
                drops.add(new ItemStack(ModItems.BLANK_TOME));
            }
            // Very rare chance for Low Level Skill Tome (1%)
            if (random.nextFloat() < 0.01f) {
                ItemStack tome = createRandomSkillTome(1, 2, random);
                if (!tome.isEmpty()) {
                    drops.add(tome);
                }
            }
        }

        // 2. Check for elite mobs (Wither Skeletons, Piglins, Shulkers)
        if (isEliteMob(type)) {
            // Rare chance for Blank Tome (5%)
            if (random.nextFloat() < 0.05f) {
                drops.add(new ItemStack(ModItems.BLANK_TOME));
            }
            // Very rare chance for High Level Skill Tome (2%)
            if (random.nextFloat() < 0.02f) {
                ItemStack tome = createRandomSkillTome(3, 5, random);
                if (!tome.isEmpty()) {
                    drops.add(tome);
                }
            }
        }

        return drops;
    }

    private static boolean isBasicMob(EntityType<?> type) {
        return type == EntityType.ZOMBIE || type == EntityType.SKELETON ||
                type == EntityType.PILLAGER || type == EntityType.ENDERMAN;
    }

    private static boolean isEliteMob(EntityType<?> type) {
        return type == EntityType.WITHER_SKELETON || type == EntityType.PIGLIN ||
                type == EntityType.SHULKER || type == EntityType.PIGLIN_BRUTE;
    }

    private static ItemStack createRandomSkillTome(int minLevel, int maxLevel, Random random) {
        var entries = new ArrayList<>(LeveledConfigStorage.getAllEntries().entrySet());
        if (entries.isEmpty())
            return ItemStack.EMPTY;

        Collections.shuffle(entries);
        for (var entry : entries) {
            var config = entry.getValue();
            if (config.lootMode != null && config.categoryId != null) {
                int level = minLevel + random.nextInt(maxLevel - minLevel + 1);
                level = Math.min(level, config.maxLevels);
                return SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, config.categoryId, entry.getKey(),
                        config.lootMode, level);
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Logic for injecting into vanilla chest loot tables.
     * minRolls/maxRolls determine how many chances we have per chest.
     */
    public static void injectChestLoot(Identifier id, LootPool.Builder builder) {
        if (id.getPath().contains("chests/village") || id.getPath().contains("chests/simple_dungeon") ||
                id.getPath().contains("chests/abandoned_mineshaft")) {

            // Basic structures: Blank Tomes (5% chance)
            builder.with(ItemEntry.builder(ModItems.BLANK_TOME).weight(5));
            // Basic structures: Lv1-2 Skill Tomes (2% chance)
            // Note: Since Skill Tomes require random level/skill, we might need a custom
            // LootFunction
            // or just add the base unconfigured tome if we can't do complex logic here.
            // For now, let's keep it simple or use a pre-configured loot table if possible.
        }

        if (id.getPath().contains("chests/nether_fortress") || id.getPath().contains("chests/end_city") ||
                id.getPath().contains("chests/ancient_city") || id.getPath().contains("chests/bastion_remnant")) {

            // Elite structures: Higher rarity or better items
            builder.with(ItemEntry.builder(ModItems.BLANK_TOME).weight(10));
        }
    }
}
