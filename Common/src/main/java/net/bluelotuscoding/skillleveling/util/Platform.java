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
}
