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

        // 1. Add custom trades from datapacks
        var loader = SkillLevelingMod.getInstance().getTradeLoader();
        List<SkillMasterTradeLoader.TradeTemplate> customTemplates = loader.getCustomTradesForTier(tier);
        if (customTemplates != null) {
            for (var template : customTemplates) {
                if (villager.getWorld().getRandom().nextFloat() < template.chance) {
                    trades.add(template.createOffer());
                }
            }
        }

        // 2. Updated slots: Increased by 2 per tier
        int minSlots = 7 + (tier - 1); // 7 at Novice (T1), 11 at Master (T5)
        int maxSlots = minSlots + 2; // 9 at Novice (T1), 13 at Master (T5)
        if (tier == 5)
            maxSlots = 14;

        // 3. Add Trade-In Offers
        addTradeInOffers(trades, tier, player);

        // 5. Add Tome Upgrade Offers (Tier 3+)
        if (tier >= 3) {
            addTomeUpgradeTrades(player, trades, tier);
        }

        // 6. Tome Probability Logic (Exactly 2 tomes total)
        float roll = villager.getWorld().getRandom().nextFloat();
        int expTomesToFill = 0;
        int skillTomesToFill = 0;

        if (roll < 0.15f) {
            expTomesToFill = 2;
        } else if (roll < 0.25f) { // 15% + 10%
            skillTomesToFill = 2;
        } else {
            expTomesToFill = 1;
            skillTomesToFill = 1;
        }

        addExpTomeTrades(player, trades, expTomesToFill, tier);
        addOverhauledDynamicTrades(player, trades, skillTomesToFill, tier);

        // 7. Filler Trades: Ensure at least minSlots are showing
        while (trades.size() < minSlots) {
            trades.add(new TradeOffer(new ItemStack(Items.EMERALD, 5), new ItemStack(Items.BOOK),
                    new ItemStack(ModItems.BLANK_TOME), 12, 1, 0.05f));
        }

        // 8. Check for Mastery message
        checkAndNotifyMastery(player);

        // 9. Limit to maxSlots
        while (trades.size() > maxSlots) {
            trades.remove(trades.size() - 1);
        }
    }

    private static void addExpTomeTrades(ServerPlayerEntity player, TradeOfferList trades, int slotsToFill, int tier) {
        var tomes = ExpTomeConfigLoader.getTomes();
        if (tomes.isEmpty())
            return;

        int added = 0;
        List<String> tomeIds = new ArrayList<>(tomes.keySet());
        Collections.shuffle(tomeIds);

        for (String tomeId : tomeIds) {
            if (added >= slotsToFill)
                break;

            var def = tomes.get(tomeId);

            // Tier-based level scaling
            int maxOfferLevel = (int) Math.round(def.maxLevels * (tier / 5.0));
            int minOfferLevel = (int) Math.round(def.maxLevels * ((tier - 1) / 5.0)) + 1;

            if (tier == 5)
                maxOfferLevel = def.maxLevels;
            maxOfferLevel = Math.max(1, Math.min(maxOfferLevel, def.maxLevels));
            minOfferLevel = Math.max(1, Math.min(minOfferLevel, maxOfferLevel));

            int levelToOffer = minOfferLevel + player.getRandom().nextInt(maxOfferLevel - minOfferLevel + 1);
            int xpAmount = def.experiencePerLevel.getCost(levelToOffer);

            ItemStack tome = net.bluelotuscoding.skillleveling.item.ExpTomeItem.createExpTome(
                    ModItems.EXP_TOME, tomeId, def.name, def.rarity, levelToOffer, xpAmount);

            int price = calculateTomePrice(player.getRandom(), levelToOffer, def.maxLevels);
            // Ensure Exp Tomes feel premium
            price = Math.max(price, 15 + (tier * 5));
            price = Math.min(64, price);

            trades.add(
                    new TradeOffer(new ItemStack(Items.EMERALD, price), new ItemStack(Items.BOOK), tome, 1, 12, 0.05f));
            added++;
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
            if (config.categoryId == null || config.lootMode == null)
                continue;

            Identifier catId = new Identifier(config.categoryId);
            int current = manager.getBaseSkillLevel(player, catId, skillId);
            int max = config.maxLevels;
            if (current < max) {
                availableSkills.add(new SkillInfo(catId, skillId, current, config.lootMode));
            }
        }

        if (availableSkills.isEmpty()) {
            return;
        }

        // PRIORITY SELECTION
        // 1. Unlearned skills (Level 0)
        List<SkillInfo> unlearned = availableSkills.stream().filter(s -> s.currentLevel == 0)
                .collect(Collectors.toList());
        // 2. Rare skills of types the player HAS learned but hasn't maxed
        List<SkillInfo> progression = availableSkills.stream().filter(s -> s.currentLevel > 0)
                .collect(Collectors.toList());

        Collections.shuffle(unlearned);
        Collections.shuffle(progression);

        List<SkillInfo> selection = new ArrayList<>();
        // Mix: 70% chance for unlearned/missing skills, 30% for progression
        for (int i = 0; i < slotsToFill * 2; i++) {
            if (player.getRandom().nextFloat() < 0.7f && !unlearned.isEmpty()) {
                selection.add(unlearned.remove(0));
            } else if (!progression.isEmpty()) {
                selection.add(progression.remove(0));
            } else if (!unlearned.isEmpty()) {
                selection.add(unlearned.remove(0));
            }
        }

        int added = 0;
        for (SkillInfo info : selection) {
            if (added >= slotsToFill)
                break;

            int maxLevel = entries.get(info.skillId).maxLevels;

            // NEW MATH-BASED TIER SPLITTING
            // Divided into 5 segments based on maxLevel
            int maxLevelForTier = (int) Math.round(maxLevel * (tier / 5.0));
            int minLevelForTier = (int) Math.round(maxLevel * ((tier - 1) / 5.0)) + 1;

            // Ensure min <= max and T5 always hits maxLevel
            if (tier == 5)
                maxLevelForTier = maxLevel;
            maxLevelForTier = Math.max(1, Math.min(maxLevelForTier, maxLevel));
            minLevelForTier = Math.max(1, Math.min(minLevelForTier, maxLevelForTier));

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
            levelToOffer = Math.min(levelToOffer, maxLevel);

            // SPECIAL UPGRADE CHANCE (Emeralds + Tome -> Upgrade)
            SkillMasterReputationLoader repLoader = SkillLevelingMod.getInstance().getReputationLoader();
            float upgradeChance = repLoader.calculateUpgradeChance(getPlayerMasteryCount(player));

            if (tier >= 3 && info.currentLevel > 0 && player.getRandom().nextFloat() < upgradeChance) {
                // ADD UPGRADE TRADE
                int emeralds = calculateTomePrice(player.getRandom(), levelToOffer, maxLevel);
                // Discounted price for upgrade
                emeralds = (int) Math.ceil(emeralds * repLoader.tomeUpgradePriceMultiplier);
                emeralds = Math.max(1, Math.min(emeralds, 64));

                ItemStack inputTome = SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, info.catId.toString(),
                        info.skillId, info.lootMode, info.currentLevel);
                ItemStack outputTome = SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, info.catId.toString(),
                        info.skillId, info.lootMode, levelToOffer);

                trades.add(new TradeOffer(new ItemStack(Items.EMERALD, emeralds), inputTome, outputTome, 1, 15, 0.05f));
            } else {
                // ADD STANDARD PURCHASE TRADE
                int price = calculateTomePrice(player.getRandom(), levelToOffer, maxLevel);
                int exp = repLoader.calculateStandardExp(tier, info.lootMode, getPlayerMasteryCount(player));

                ItemStack tome = SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, info.catId.toString(), info.skillId,
                        info.lootMode, levelToOffer);
                trades.add(
                        new TradeOffer(new ItemStack(Items.EMERALD, price), new ItemStack(Items.BOOK), tome, 1, exp,
                                0.05f));
            }
            added++;
        }
    }

    private static int calculateTomePrice(net.minecraft.util.math.random.Random random, int level, int maxLevels) {
        SkillMasterReputationLoader repLoader = SkillLevelingMod.getInstance().getReputationLoader();

        // Base rate: 3-10 emeralds per level
        int baseRate = 3 + random.nextInt(8);

        // Pre-discount cap: level * baseRate capped at 64
        int baseCost = Math.min(level * baseRate, 64);

        // Scaling factor: 1.2 (120%) at level 1, dropping to 0.5 (50%) at maxLevel
        float progress = (float) (level - 1) / Math.max(1, maxLevels - 1);
        float multiplier = repLoader.tomePriceMultiplierLevel1
                + (progress * (repLoader.tomePriceMultiplierMaxLevel - repLoader.tomePriceMultiplierLevel1));

        int price = (int) Math.ceil(baseCost * multiplier);

        // Ensure minimum 3-10 for level 1 trades (with the 1.2x multiplier)
        if (level == 1) {
            price = Math.max(price, (int) Math.ceil(baseRate * repLoader.tomePriceMultiplierLevel1));
        }

        return Math.max(1, Math.min(price, 64));
    }

    private static void addTradeInOffers(TradeOfferList trades, int tier, ServerPlayerEntity player) {
        // Keep any special trade-in logic here if not covered by JSON
        // For example, trading lower-tier items for materials, etc.
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

    /**
     * Adds upgrade trades: Emeralds + Tome(Level L) -> Tome(Level L+1).
     */
    private static void addTomeUpgradeTrades(ServerPlayerEntity player, TradeOfferList trades, int tier) {
        Map<String, LeveledConfigStorage.LeveledConfig> entries = LeveledConfigStorage.getAllEntries();
        SkillLevelingManager manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
        SkillMasterReputationLoader repLoader = SkillLevelingMod.getInstance().getReputationLoader();
        int added = 0;
        int maxUpgrades = 3;

        for (var entry : entries.entrySet()) {
            if (added >= maxUpgrades)
                break;

            String skillId = entry.getKey();
            var config = entry.getValue();
            if (config.categoryId == null || config.lootMode == null)
                continue;

            Identifier catId = new Identifier(config.categoryId);
            int currentLevel = manager.getBaseSkillLevel(player, catId, skillId);

            // If player has the skill at level L, offer Level L+1 tome upgrade
            if (currentLevel > 0 && currentLevel < config.maxLevels) {
                int levelToOffer = currentLevel + 1;
                int price = calculateTomePrice(player.getRandom(), levelToOffer, config.maxLevels);
                price = (int) Math.ceil(price * repLoader.tomeUpgradePriceMultiplier);
                price = Math.max(1, Math.min(price, 64));

                ItemStack inputTome = SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, config.categoryId, skillId,
                        config.lootMode, currentLevel);
                ItemStack outputTome = SkillTomeItem.createSkillTome(ModItems.SKILL_TOME, config.categoryId, skillId,
                        config.lootMode, levelToOffer);

                trades.add(new TradeOffer(new ItemStack(Items.EMERALD, price), inputTome, outputTome, 1, 10, 0.05f));
                added++;
            }
        }
    }

    /**
     * Minimal proxy trade to satisfy Minecraft's profession registration
     * requirements.
     * Actual trades are generated dynamically on interaction.
     */
    public static TradeOffer createLevel1ProxyTrade(ServerPlayerEntity player, TradeOfferList existingTrades) {
        return new TradeOffer(new ItemStack(Items.EMERALD, 5), new ItemStack(Items.BOOK),
                new ItemStack(ModItems.BLANK_TOME), 12, 2, 0.05f);
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
