package net.bluelotuscoding.skillleveling.loot;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
import net.bluelotuscoding.skillleveling.item.SkillTomeItem;
import net.bluelotuscoding.skillleveling.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating ItemStacks from UnifiedLootConfig entries.
 */
public final class LootStackFactory {

    public static ItemStack createStack(UnifiedLootConfig.LootEntry entry, Random random) {
        if ("skill_tome".equals(entry.type())) {
            return createSkillTome(entry, random);
        }

        if ("skill_charm".equals(entry.type())) {
            return createSkillCharm(entry, random);
        }

        Identifier itemId = new Identifier(entry.name());
        var optionalItem = net.minecraft.registry.Registries.ITEM.getOrEmpty(itemId);
        if (optionalItem.isEmpty()) {
            SkillLevelingMod.getInstance().getLogger().info("[LootStackFactory] Item not found: " + entry.name());
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(optionalItem.get());
        entry.nbt().ifPresent(nbtStr -> {
            try {
                stack.setNbt(net.minecraft.nbt.StringNbtReader.parse(nbtStr));
            } catch (Exception e) {
                SkillLevelingMod.getInstance().getLogger().error("Error parsing NBT: " + e.getMessage());
            }
        });

        return stack;
    }

    private static ItemStack createSkillTome(UnifiedLootConfig.LootEntry entry, Random random) {
        if (entry.skill().isPresent()) {
            String skillId = entry.skill().get();
            var config = LeveledConfigStorage.get(skillId);
            if (config != null) {
                int maxLevel = SkillLevelingMod.getInstance().getSkillLevelingManager().getMaxLevel(
                        new Identifier(config.categoryId),
                        skillId);
                int effectiveMax = Math.min(entry.maxLevel(), maxLevel);
                int effectiveMin = Math.min(entry.minLevel(), effectiveMax);

                int level = effectiveMin;
                if (effectiveMax > effectiveMin) {
                    level += random.nextInt(effectiveMax - effectiveMin + 1);
                }
                level = Math.max(1, level);

                SkillLevelingMod.getInstance().getLogger()
                        .info("[LootStackFactory] Creating specific skill tome: " + skillId + " Lvl " + level);
                return SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, config.categoryId, skillId, config.lootMode,
                        level);
            }
        } else if (!entry.skills().isEmpty()) {
            int totalWeight = entry.skills().stream().mapToInt(UnifiedLootConfig.SkillPoolEntry::weight).sum();
            if (totalWeight > 0) {
                int r = random.nextInt(totalWeight);
                int current = 0;
                for (UnifiedLootConfig.SkillPoolEntry spe : entry.skills()) {
                    current += spe.weight();
                    if (r < current) {
                        var config = LeveledConfigStorage.get(spe.skill());
                        if (config != null) {
                            int maxLevel = SkillLevelingMod.getInstance().getSkillLevelingManager()
                                    .getMaxLevel(new Identifier(config.categoryId), spe.skill());
                            int effectiveMax = Math.min(entry.maxLevel(), maxLevel);
                            int effectiveMin = Math.min(entry.minLevel(), effectiveMax);

                            int level = effectiveMin;
                            if (effectiveMax > effectiveMin) {
                                level += random.nextInt(effectiveMax - effectiveMin + 1);
                            }
                            level = Math.max(1, level);

                            SkillLevelingMod.getInstance().getLogger().info(
                                    "[LootStackFactory] Creating pool skill tome: " + spe.skill() + " Lvl " + level);
                            return SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, config.categoryId, spe.skill(),
                                    config.lootMode, level);
                        }
                        break;
                    }
                }
            }
        } else {
            // Randomly choose from all skills if no filter
            var allSkills = LeveledConfigStorage.getAllEntries();
            if (!allSkills.isEmpty()) {
                List<String> keys = new ArrayList<>(allSkills.keySet());
                String skillId = keys.get(random.nextInt(keys.size()));
                var config = allSkills.get(skillId);

                int maxLevel = SkillLevelingMod.getInstance().getSkillLevelingManager().getMaxLevel(
                        new Identifier(config.categoryId),
                        skillId);
                int effectiveMax = Math.min(entry.maxLevel(), maxLevel);
                int effectiveMin = Math.min(entry.minLevel(), effectiveMax);

                int level = effectiveMin;
                if (effectiveMax > effectiveMin) {
                    level += random.nextInt(effectiveMax - effectiveMin + 1);
                }
                level = Math.max(1, level);

                SkillLevelingMod.getInstance().getLogger()
                        .info("[LootStackFactory] Creating random skill tome: " + skillId + " Lvl " + level);
                return SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, config.categoryId, skillId, config.lootMode,
                        level);
            }
        }
        SkillLevelingMod.getInstance().getLogger().info("[LootStackFactory] Falling back to default skill tome.");
        return new ItemStack(ModItems.SKILL_TOME);
    }

    private static ItemStack createSkillCharm(UnifiedLootConfig.LootEntry entry, Random random) {
        ItemStack stack = new ItemStack(ModItems.SKILL_CHARM);
        // Note: Skill Charms are typically left empty and imbued by the skill_imbue
        // modifier
        // unless specific logic is added here.
        return stack;
    }
}
