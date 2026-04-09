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

    @Override
    public net.minecraft.nbt.NbtCompound getPersistentData(Object player) {
        // Standard Fabric doesn't have persistent data on entities without CC or
        // similar.
        // For now we return a new compound to satisfy the interface and prevent
        // crashes.
        return new net.minecraft.nbt.NbtCompound();
    }

    @Override
    public void resetEpicClassStats(Object player) {
        // Epic Class is Forge-only
    }

    @Override
    public void setEpicClassStatPoints(Object player, int amount) {
        // Epic Class is Forge-only
    }

    @Override
    public void syncEpicClassLevel(Object player, int level, int xp,
            int neededXp, int lastGain) {
        // No-op on Fabric
    }

    @Override
    public int getPufferfishLevel(Object player,
            net.minecraft.util.Identifier categoryId) {
        return 0;
    }

    @Override
    public int getPufferfishExperience(Object player,
            net.minecraft.util.Identifier categoryId) {
        return 0;
    }

    @Override
    public int getPufferfishNeededExperience(Object player,
            net.minecraft.util.Identifier categoryId) {
        return 0;
    }

    @Override
    public void addPufferfishExperience(Object player, int amount) {
        // No-op on Fabric
    }

    @Override
    public void addPufferfishExperience(Object player, net.minecraft.util.Identifier categoryId, int amount) {
        // No-op on Fabric
    }

    @Override
    public String getEpicClassName(Object player) {
        return null;
    }

    @Override
    public int getEpicClassPlayerLevel(Object player) {
        // Epic Class is Forge-only for now
        return 0;
    }

    @Override
    public int getPufferfishPoints(Object player,
            net.minecraft.util.Identifier categoryId) {
        return 0;
    }

    @Override
    public void syncCustomClassName(Object player) {
        // No-op on Fabric
    }

    @Override
    public void setCustomClassName(Object player, String name) {
        // Fabric implementation pending Epic Classes integration
    }

    @Override
    public void sendAdvanceClassScreen(Object player, String parentClassId) {
        // Fabric implementation pending Epic Classes networking integration
    }

    @Override
    public boolean isClient() {
        return net.fabricmc.loader.api.FabricLoader.getInstance()
                .getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
    }

    @Override
    public void cleanupPlayerData(Object player) {
        // No-op on Fabric
    }

    @Override
    public void cleanupWorldData(Object level) {
        // No-op on Fabric
    }
}
