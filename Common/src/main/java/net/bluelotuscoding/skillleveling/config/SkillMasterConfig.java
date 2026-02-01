package net.bluelotuscoding.skillleveling.config;

/**
 * Configuration for Skill Master reputation (experience) gain.
 * These values can be adjusted to make levelling the villager harder or easier.
 */
public class SkillMasterConfig {

    // Base experience gained for any trade
    public static int baseExp = 5;

    // Bonus experience for specific loot modes
    public static int bothModeBonus = 3;
    public static int imbueOnlyBonus = 5;

    // Experience multiplier per villager tier (1-5)
    public static int tierMultiplier = 2;

    // Static experience values for specific items
    public static int sigilProxyExp = 5;
    public static int tomeOfClearMindExp = 10;
    public static int tomeOfCleansingExp = 15;
    public static int sigilOfImbuementExp = 50;
    public static int tomeOfCleansingUpgradeExp = 25;

    /**
     * Calculates the experience for a standard skill tome trade.
     */
    public static int calculateStandardExp(int tier, String lootMode) {
        int exp = baseExp + (tier * tierMultiplier);
        if ("imbue_only".equals(lootMode)) {
            exp += imbueOnlyBonus;
        } else if ("both".equals(lootMode)) {
            exp += bothModeBonus;
        }
        return exp;
    }
}
