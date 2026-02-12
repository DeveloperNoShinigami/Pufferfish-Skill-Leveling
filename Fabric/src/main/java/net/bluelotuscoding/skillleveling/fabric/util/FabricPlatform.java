package net.bluelotuscoding.skillleveling.fabric.util;

import net.bluelotuscoding.skillleveling.util.Platform;
import net.minecraft.entity.effect.StatusEffectInstance;

public class FabricPlatform implements Platform {
    @Override
    public void makePersistent(StatusEffectInstance instance) {
        // Fabric doesn't have curative items in StatusEffectInstance by default.
        // We handle persistence (milk immunity) via a common mixin if needed,
        // but for now, we leave this empty.
    }

    @Override
    public boolean isFabric() {
        return true;
    }

    @Override
    public boolean isForge() {
        return false;
    }
}
