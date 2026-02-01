package net.bluelotuscoding.skillleveling.data;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.item.SkillTomeItem;
import net.bluelotuscoding.skillleveling.manager.SkillLevelingManager;
import net.bluelotuscoding.skillleveling.registry.ModItems;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.puffish.skillsmod.api.SkillsAPI;

import java.util.*;

/**
 * Generates dynamic trades for the Skill Master villager based on player
 * progression.
 */
public class SkillMasterTradeProvider {

    public static void fillTrades(VillagerEntity villager, TradeOfferList trades, int tier, ServerPlayerEntity player) {
        SkillLevelingMod.getInstance().getLogger().info("Filling trades for Skill Master at tier " + tier);
        // 1. Add custom trades from datapacks (if any)
        var loader = SkillLevelingMod.getInstance().getTradeLoader();
        List<SkillMasterTradeLoader.TradeTemplate> customTemplates = loader.getCustomTradesForTier(tier);
        SkillLevelingMod.getInstance().getLogger()
                .info("Found " + customTemplates.size() + " custom templates for tier " + tier);
        for (var template : customTemplates) {
            trades.add(template.createOffer());
        }

        // 2. Add dynamic "Skill Tome" trades
        addDynamicTomeTrades(trades, tier, player);

        // 3. At higher tiers, add utility items if not already added by custom trades
        addUtilityTrades(trades, tier);

        // 4. FALLBACK: Ensure the villager ALWAYS has at least one trade.
        // Minecraft rejects professions that have 0 trades at Level 1.
        if (trades.isEmpty()) {
            SkillLevelingMod.getInstance().getLogger()
                    .info("No dynamic trades generated, adding fallback proxy trade.");
            trades.add(createLevel1ProxyTrade());
        }

        SkillLevelingMod.getInstance().getLogger().info("Final trade count: " + trades.size());
    }

    /**
     * Creates a fallback trade for Level 1 to ensure the profession is valid.
     */
    public static TradeOffer createLevel1ProxyTrade() {
        return new TradeOffer(
                new ItemStack(Items.EMERALD, 48),
                new ItemStack(Items.BOOK, 1),
                new ItemStack(ModItems.TOME_OF_PROGRESSION, 1),
                12, 15, 0.05f);
    }

    private static void addDynamicTomeTrades(TradeOfferList trades, int tier, ServerPlayerEntity player) {
        SkillLevelingManager manager = SkillLevelingMod.getInstance().getSkillLevelingManager();

        // Find skills that the player has unlocked but not yet mastered
        List<SkillInfo> interestingSkills = new ArrayList<>();

        SkillsAPI.streamCategories().forEach(category -> {
            Identifier catId = category.getId();
            category.streamSkills().forEach(skill -> {
                String skillId = skill.getId();
                // Check if unlocked in base mod
                if (skill.getState(player) == net.puffish.skillsmod.api.Skill.State.UNLOCKED) {
                    int currentLevel = manager.getBaseSkillLevel(player, catId, skillId);
                    int maxLevel = manager.getMaxLevel(catId, skillId);

                    if (currentLevel < maxLevel && maxLevel > 1) {
                        interestingSkills.add(new SkillInfo(catId, skillId, currentLevel, maxLevel));
                    }
                }
            });
        });

        // Shuffle and pick up to 3-5 interesting skills based on tier
        Collections.shuffle(interestingSkills);
        int maxDynamicTrades = tier + 1; // 2 at Novice, 6 at Master

        interestingSkills.stream().limit(maxDynamicTrades).forEach(info -> {
            ItemStack tome = SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, info.catId.toString(), info.skillId,
                    "both", info.currentLevel + 1);

            // Cost scales with level
            int emeraldCost = 15 + ((info.currentLevel + 1) * 5);
            trades.add(new TradeOffer(
                    new ItemStack(Items.EMERALD, Math.min(64, emeraldCost)),
                    new ItemStack(Items.BOOK),
                    tome,
                    12, // max uses
                    10, // experience
                    0.05f // multiplier
            ));
        });

        // If NO interesting skills (player has 0 skills), add some "Featured" starter
        // tomes
        if (interestingSkills.isEmpty()) {
            addFeaturedStarterTomes(trades, tier);
        }
    }

    private static void addFeaturedStarterTomes(TradeOfferList trades, int tier) {
        // Find some skills that have loot_mode set and offer them at level 1
        var entriesMap = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.getAllEntries();
        int count = 0;
        for (var entry : entriesMap.entrySet()) {
            var config = entry.getValue();
            if (config.lootMode != null && config.categoryId != null && !"imbue_only".equals(config.lootMode)) {
                ItemStack tome = SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, config.categoryId, entry.getKey(),
                        config.lootMode, 1);
                trades.add(new TradeOffer(
                        new ItemStack(Items.EMERALD, 20),
                        new ItemStack(Items.BOOK),
                        tome,
                        12, 5, 0.05f));
                count++;
                if (count >= 3)
                    break;
            }
        }
    }

    private static void addUtilityTrades(TradeOfferList trades, int tier) {
        // Tier 1+: Tome of Progression & Tome of Clear Mind
        if (tier >= 1) {
            trades.add(new TradeOffer(
                    new ItemStack(Items.EMERALD, 48),
                    new ItemStack(Items.BOOK),
                    new ItemStack(ModItems.TOME_OF_PROGRESSION),
                    12, 15, 0.05f));

            trades.add(new TradeOffer(
                    new ItemStack(Items.EMERALD, 32),
                    new ItemStack(Items.BOOK),
                    new ItemStack(ModItems.TOME_OF_CLEAR_MIND),
                    12, 10, 0.05f));
        }

        // Tier 2+: Tome of Cleansing (Slot 0)
        if (tier >= 2) {
            trades.add(new TradeOffer(
                    new ItemStack(Items.EMERALD, 40),
                    new ItemStack(Items.GHAST_TEAR),
                    new ItemStack(ModItems.TOME_OF_CLEANSING),
                    5, 20, 0.05f));
        }

        // Tier 3+: Greater Clear Mind & Tome of Cleansing II
        if (tier >= 3) {
            trades.add(new TradeOffer(
                    new ItemStack(Items.EMERALD, 32),
                    new ItemStack(ModItems.TOME_OF_CLEAR_MIND),
                    new ItemStack(ModItems.TOME_OF_GREATER_CLEAR_MIND),
                    8, 20, 0.05f));

            trades.add(new TradeOffer(
                    new ItemStack(Items.EMERALD, 40),
                    new ItemStack(Items.GHAST_TEAR),
                    new ItemStack(ModItems.TOME_OF_CLEANSING_2),
                    5, 20, 0.05f));
        }

        // Tier 4+: Tome of Cleansing III
        if (tier >= 4) {
            trades.add(new TradeOffer(
                    new ItemStack(Items.EMERALD, 40),
                    new ItemStack(Items.GHAST_TEAR),
                    new ItemStack(ModItems.TOME_OF_CLEANSING_3),
                    5, 25, 0.05f));
        }

        // Tier 5: Sigil of Imbuement
        if (tier >= 5) {
            trades.add(new TradeOffer(
                    new ItemStack(Items.EMERALD, 64),
                    new ItemStack(Items.NETHER_STAR),
                    new ItemStack(ModItems.SIGIL_OF_IMBUEMENT),
                    3, 40, 0.05f));
        }
    }

    private static class SkillInfo {
        final Identifier catId;
        final String skillId;
        final int currentLevel;
        final int maxLevel;

        SkillInfo(Identifier catId, String skillId, int currentLevel, int maxLevel) {
            this.catId = catId;
            this.skillId = skillId;
            this.currentLevel = currentLevel;
            this.maxLevel = maxLevel;
        }
    }
}
