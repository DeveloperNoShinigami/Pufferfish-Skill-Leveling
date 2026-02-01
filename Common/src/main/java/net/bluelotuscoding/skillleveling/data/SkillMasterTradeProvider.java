package net.bluelotuscoding.skillleveling.data;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.item.SkillTomeItem;
import net.bluelotuscoding.skillleveling.manager.SkillLevelingManager;
import net.bluelotuscoding.skillleveling.registry.ModItems;
import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates dynamic trades for the Skill Master villager based on player
 * progression.
 */
public class SkillMasterTradeProvider {

    public static void fillTrades(VillagerEntity villager, TradeOfferList trades, int tier, ServerPlayerEntity player) {
        SkillLevelingMod.getInstance().getLogger().info("Filling trades for Skill Master at tier " + tier);

        // 1. Add custom trades from datapacks
        var loader = SkillLevelingMod.getInstance().getTradeLoader();
        List<SkillMasterTradeLoader.TradeTemplate> customTemplates = loader.getCustomTradesForTier(tier);
        if (customTemplates != null) {
            for (var template : customTemplates) {
                trades.add(template.createOffer());
            }
        }

        // 2. Determine dynamic slot count based on tier (5-12 range)
        int minSlots = 5 + (tier - 1); // 5 at Novice (T1), 9 at Master (T5)
        int maxSlots = minSlots + 2;
        if (tier == 5)
            maxSlots = 12;

        // 3. Add Utility Trades
        addOverhauledUtilityTrades(trades, tier, player);
        SkillLevelingMod.getInstance().getLogger().info("Added utility trades. Total count: " + trades.size());

        // 4. Add Trade-In Offers
        addTradeInOffers(trades, tier, player);

        // 5. Add Tome Upgrade Offers (Tier 3+)
        if (tier >= 3) {
            addTomeUpgradeTrades(player, trades, tier);
        }

        // 6. Randomized Skill Trades (Remaining slots)
        int remainingSlots = maxSlots - trades.size();
        if (remainingSlots > 0) {
            addOverhauledDynamicTrades(player, trades, remainingSlots, tier);
        }

        // 6. Check for Mastery message
        checkAndNotifyMastery(player);

        // 7. Limit to maxSlots
        while (trades.size() > maxSlots) {
            trades.remove(trades.size() - 1);
        }
    }

    private static void addOverhauledDynamicTrades(ServerPlayerEntity player, TradeOfferList trades, int slotsToFill,
            int tier) {
        List<SkillInfo> availableSkills = new ArrayList<>();
        Map<String, LeveledConfigStorage.LeveledConfig> entries = LeveledConfigStorage.getAllEntries();
        SkillLevelingManager manager = SkillLevelingMod.getInstance().getSkillLevelingManager();

        for (var entry : entries.entrySet()) {
            String skillId = entry.getKey();
            var config = entry.getValue();
            if (config.categoryId == null)
                continue;

            Identifier catId = new Identifier(config.categoryId);
            int current = manager.getBaseSkillLevel(player, catId, skillId);
            int max = config.maxLevels;
            if (current < max) {
                availableSkills.add(new SkillInfo(catId, skillId, current, config.lootMode));
            }
        }

        if (availableSkills.isEmpty()) {
            addFeaturedStarterTomes(trades, tier);
            return;
        }

        // Priority: Unlearned skills first
        List<SkillInfo> priorityList = availableSkills.stream()
                .filter(s -> s.currentLevel == 0)
                .collect(Collectors.toList());
        Collections.shuffle(priorityList);

        List<SkillInfo> fallbackList = availableSkills.stream()
                .filter(s -> s.currentLevel > 0)
                .collect(Collectors.toList());
        Collections.shuffle(fallbackList);

        List<SkillInfo> selection = new ArrayList<>(priorityList);
        selection.addAll(fallbackList);

        int added = 0;
        for (SkillInfo info : selection) {
            if (added >= slotsToFill)
                break;

            int maxLevel = entries.get(info.skillId).maxLevels;

            // TIER-BASED LEVEL SCALING
            // Mapping 1-5 Tiers to 1-maxLevel
            int minLevelForTier = (int) Math.ceil(maxLevel * ((tier - 1) / 5.0)) + 1;
            int maxLevelForTier = (int) Math.ceil(maxLevel * (tier / 5.0));

            // Difference-based availability: only offer if player is below this tier's
            // range
            if (info.currentLevel >= maxLevelForTier) {
                continue; // Player already beyond this villager's expertise
            }

            // Target level is between max(current+1, minForTier) and maxForTier
            int minPossible = Math.max(info.currentLevel + 1, minLevelForTier);
            if (minPossible > maxLevelForTier) {
                continue;
            }

            int levelToOffer = minPossible + player.getRandom().nextInt(maxLevelForTier - minPossible + 1);

            // Price scales primarily with emeralds
            int basePrice = 8 + (levelToOffer * 4);
            int price = basePrice + player.getRandom().nextInt(tier + 1);
            price = Math.min(price, 64);

            int masteryCount = getPlayerMasteryCount(player);
            // Use reputationLoader for standard tome trades
            int exp = SkillLevelingMod.getInstance().getReputationLoader().calculateStandardExp(tier,
                    info.lootMode, masteryCount);

            ItemStack tome = SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, info.catId.toString(), info.skillId,
                    info.lootMode, levelToOffer);
            trades.add(
                    new TradeOffer(new ItemStack(Items.EMERALD, price), new ItemStack(Items.BOOK), tome, 1, exp,
                            0.05f));
            added++;
        }
    }

    private static void addOverhauledUtilityTrades(TradeOfferList trades, int tier, ServerPlayerEntity player) {
        int masteryCount = getPlayerMasteryCount(player);
        var repLoader = SkillLevelingMod.getInstance().getReputationLoader();

        // Level 1: Proxy / Variety Trade
        if (tier >= 1) {
            // ONLY add if not already overridden by datapack
            trades.add(createLevel1ProxyTrade(player, trades));
        }

        if (tier >= 2) {
            if (!isItemAlreadyOffered(trades, ModItems.TOME_OF_CLEAR_MIND)) {
                trades.add(new TradeOffer(new ItemStack(Items.EMERALD, 16), new ItemStack(Items.BOOK),
                        new ItemStack(ModItems.TOME_OF_CLEAR_MIND), 12,
                        repLoader.applyMasteryToStatic(repLoader.tomeOfClearMindExp, masteryCount), 0.05f));
            }
            if (!isItemAlreadyOffered(trades, ModItems.TOME_OF_CLEANSING)) {
                trades.add(new TradeOffer(new ItemStack(Items.EMERALD, 24), new ItemStack(Items.GHAST_TEAR),
                        new ItemStack(ModItems.TOME_OF_CLEANSING), 5,
                        repLoader.applyMasteryToStatic(repLoader.tomeOfCleansingExp, masteryCount), 0.05f));
            }
        }

        if (tier >= 5) {
            // Rare chance (25%) for a Sigil at higher tiers if not already present
            if (!isItemAlreadyOffered(trades, ModItems.SIGIL_OF_IMBUEMENT) && player.getRandom().nextFloat() < 0.25f) {
                int emeralds = 25 + player.getRandom().nextInt(6); // 25-30
                trades.add(new TradeOffer(new ItemStack(Items.EMERALD, emeralds), new ItemStack(Items.GOLD_BLOCK),
                        new ItemStack(ModItems.SIGIL_OF_IMBUEMENT), 2,
                        repLoader.applyMasteryToStatic(repLoader.sigilOfImbuementExp, masteryCount), 0.05f));
            }
        }
    }

    private static void addTradeInOffers(TradeOfferList trades, int tier, ServerPlayerEntity player) {
        if (tier >= 3) {
            int masteryCount = getPlayerMasteryCount(player);
            var repLoader = SkillLevelingMod.getInstance().getReputationLoader();
            trades.add(new TradeOffer(new ItemStack(ModItems.TOME_OF_CLEANSING), new ItemStack(Items.EMERALD, 16),
                    new ItemStack(ModItems.TOME_OF_CLEANSING_2), 5,
                    repLoader.applyMasteryToStatic(repLoader.tomeOfCleansingUpgradeExp, masteryCount), 0.05f));
        }
    }

    private static void checkAndNotifyMastery(ServerPlayerEntity player) {
        int masterSkills = getPlayerMasteryCount(player);

        if (masterSkills >= 3) {
            player.sendMessage(net.minecraft.text.Text
                    .literal("§6[Skill Master]§f You have shown true dedication. I offer my finest wares."), false);
        }
    }

    public static int getPlayerMasteryCount(ServerPlayerEntity player) {
        Map<String, LeveledConfigStorage.LeveledConfig> entries = LeveledConfigStorage.getAllEntries();
        SkillLevelingManager manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
        int count = 0;

        for (var entry : entries.entrySet()) {
            String skillId = entry.getKey();
            var config = entry.getValue();
            if (config.categoryId == null)
                continue;

            Identifier catId = new Identifier(config.categoryId);
            if (manager.getBaseSkillLevel(player, catId, skillId) >= config.maxLevels) {
                count++;
            }
        }
        return count;
    }

    private static void addFeaturedStarterTomes(TradeOfferList trades, int tier) {
        var entriesMap = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.getAllEntries();
        int count = 0;
        for (var entry : entriesMap.entrySet()) {
            var config = entry.getValue();
            if (config.lootMode != null && config.categoryId != null && !"imbue_only".equals(config.lootMode)) {
                ItemStack tome = SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, config.categoryId, entry.getKey(),
                        config.lootMode, 1);
                trades.add(
                        new TradeOffer(new ItemStack(Items.EMERALD, 5), new ItemStack(Items.BOOK), tome, 12, 5, 0.05f));
                count++;
                if (count >= 3)
                    break;
            }
        }
    }

    public static TradeOffer createLevel1ProxyTrade(ServerPlayerEntity player, TradeOfferList existingTrades) {
        int masteryCount = (player != null) ? getPlayerMasteryCount(player) : 0;
        var repLoader = SkillLevelingMod.getInstance().getReputationLoader();

        // Use Minecraft's random if player is present, otherwise use Java's Random
        java.util.Random random = new java.util.Random();
        if (player != null) {
            // Note: In 1.20.1, player.getRandom() returns a RandomSource
            // For simplicity in this logic, we'll just use java.util.Random
            // but seeded by player's random if needed, or just standard random.
        }

        // POOL OF DYNAMIC TRADES
        List<java.util.function.Supplier<TradeOffer>> pool = new ArrayList<>();

        // 1. Sigil of Imbuement (Only if not already offered)
        if (!isItemAlreadyOffered(existingTrades, ModItems.SIGIL_OF_IMBUEMENT)) {
            pool.add(() -> {
                int emeralds = 16 + random.nextInt(7); // 16-22
                int gold = 2 + random.nextInt(3); // 2-4 Gold Ingots
                int uses = 3 + random.nextInt(3); // 3-5 uses
                return new TradeOffer(new ItemStack(Items.EMERALD, emeralds), new ItemStack(Items.GOLD_INGOT, gold),
                        new ItemStack(ModItems.SIGIL_OF_IMBUEMENT), uses,
                        repLoader.applyMasteryToStatic(repLoader.sigilProxyExp, masteryCount), 0.05f);
            });
        }

        // Pick one randomly
        if (!pool.isEmpty()) {
            return pool.get(random.nextInt(pool.size())).get();
        }

        // Final fallback: emeralds for book
        return new TradeOffer(new ItemStack(Items.EMERALD, 16), ItemStack.EMPTY, new ItemStack(Items.BOOK), 12, 5,
                0.05f);
    }

    /**
     * Adds upgrade trades: Emeralds + Tome(Level L) -> Tome(Level L+1).
     */
    private static void addTomeUpgradeTrades(ServerPlayerEntity player, TradeOfferList trades, int tier) {
        Map<String, LeveledConfigStorage.LeveledConfig> entries = LeveledConfigStorage.getAllEntries();
        SkillLevelingManager manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
        int added = 0;
        int maxUpgrades = 3;

        for (var entry : entries.entrySet()) {
            if (added >= maxUpgrades)
                break;

            String skillId = entry.getKey();
            var config = entry.getValue();
            if (config.categoryId == null)
                continue;

            Identifier catId = new Identifier(config.categoryId);
            int currentLevel = manager.getBaseSkillLevel(player, catId, skillId);

            // If player has the skill at level L, offer Level L+1 tome upgrade
            if (currentLevel > 0 && currentLevel < config.maxLevels) {
                int emeralds = 8 + (currentLevel * 8); // Scale with level

                ItemStack inputTome = SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, config.categoryId, skillId,
                        config.lootMode, currentLevel);
                ItemStack outputTome = SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, config.categoryId, skillId,
                        config.lootMode, currentLevel + 1);

                trades.add(new TradeOffer(new ItemStack(Items.EMERALD, emeralds), inputTome, outputTome, 1, 10, 0.05f));
                added++;
            }
        }
    }

    private static boolean isItemAlreadyOffered(TradeOfferList trades, net.minecraft.item.Item item) {
        if (trades == null)
            return false;
        for (var offer : trades) {
            if (offer.getSellItem().getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private static class SkillInfo {
        final Identifier catId;
        final String skillId;
        final int currentLevel;
        final String lootMode;

        SkillInfo(Identifier catId, String skillId, int currentLevel, String lootMode) {
            this.catId = catId;
            this.skillId = skillId;
            this.currentLevel = currentLevel;
            this.lootMode = lootMode;
        }
    }
}
