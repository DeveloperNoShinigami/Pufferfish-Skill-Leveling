package net.bluelotuscoding.skillleveling.forge.util;

import net.bluelotuscoding.skillleveling.util.Platform;
import net.minecraft.entity.effect.StatusEffectInstance;

public class ForgePlatform implements Platform {
    @Override
    public void makePersistent(StatusEffectInstance instance) {
        // Forge extension: status effects can have curative items (like milk)
        instance.getCurativeItems().clear();
    }

    @Override
    public boolean isFabric() {
        return false;
    }

    @Override
    public boolean isForge() {
        return true;
    }
}
