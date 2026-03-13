package net.bluelotuscoding.skillleveling.client;

/**
 * Stores synchronized NBT data from Pufferfish on the client side
 * for use in HUD scaling and other visual overrides.
 */
public final class ClientCustomNbtState {
    private static int level = 0;
    private static int xp = 0;
    private static int xpNeeded = 0;

    private ClientCustomNbtState() {
    }

    public static void update(int newLevel, int newXp, int newXpNeeded) {
        level = newLevel;
        xp = newXp;
        xpNeeded = newXpNeeded;
    }

    public static int getLevel() {
        return level;
    }

    public static int getXp() {
        return xp;
    }

    public static int getXpNeeded() {
        return xpNeeded;
    }

    /**
     * Resets state on world leave.
     */
    public static void reset() {
        level = 0;
        xp = 0;
        xpNeeded = 0;
    }
}
