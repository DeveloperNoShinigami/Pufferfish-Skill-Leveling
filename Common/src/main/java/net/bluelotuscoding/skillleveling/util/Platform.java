package net.bluelotuscoding.skillleveling.util;

import net.minecraft.entity.effect.StatusEffectInstance;

/**
 * Interface for platform-specific operations (Forge vs Fabric)
 */
public interface Platform {
        /**
         * Makes a status effect persistent (e.g. by clearing curative items on Forge)
         */
        void makePersistent(StatusEffectInstance instance);

        /**
         * Checks if the current environment is Fabric
         */
        boolean isFabric();

        /**
         * Checks if the current environment is Forge
         */
        boolean isForge();

        /**
         * Gets or creates persistent data for a player (Forge specific, dummy on
         * Fabric)
         */
        net.minecraft.nbt.NbtCompound getPersistentData(Object player);

        /**
         * Resets Epic Class stats and refunds points (Forge only)
         */
        void resetEpicClassStats(Object player);

        /**
         * Sets available Epic Class stat points (Forge only)
         */
        void setEpicClassStatPoints(Object player, int amount);

        /**
         * Synchronizes Epic Class level and XP from Pufferfish (Forge only)
         */
        void syncEpicClassLevel(Object player, int level, int xp,
                        int lastGain);

        /**
         * Gets the current Pufferfish level for a player and category.
         */
        int getPufferfishLevel(Object player,
                        net.minecraft.util.Identifier categoryId);

        /**
         * Gets the current Pufferfish experience for a player and category.
         */
        int getPufferfishExperience(Object player,
                        net.minecraft.util.Identifier categoryId);

        /**
         * Adds experience to a player's active Pufferfish category.
         */
        void addPufferfishExperience(Object player, int amount);

        /**
         * Gets the current Epic Class name for a player (Forge only)
         */
        String getEpicClassName(Object player);

        /**
         * Gets the total Pufferfish points earned for a player and category.
         */
        int getPufferfishPoints(Object player,
                        net.minecraft.util.Identifier categoryId);

        /**
         * Explicitly syncs the custom class name to the client (Forge only).
         */
        void syncCustomClassName(Object player);

        /**
         * Updates the custom class name in persistent storage (Forge only).
         */
        void setCustomClassName(Object player, String name);

        /**
         * Sends a network packet to the player to open the class advancement screen.
         */
        void sendAdvanceClassScreen(Object player, String parentClassId);
}
