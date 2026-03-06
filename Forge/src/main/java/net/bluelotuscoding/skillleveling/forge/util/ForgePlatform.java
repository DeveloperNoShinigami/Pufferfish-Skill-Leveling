package net.bluelotuscoding.skillleveling.forge.util;

import net.bluelotuscoding.skillleveling.util.Platform;
import net.minecraft.entity.player.PlayerEntity;
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

    @Override
    public net.minecraft.nbt.NbtCompound getPersistentData(Object player) {
        // In Forge with Yarn mappings, we need to cast to IForgeEntity to access
        // getPersistentData()
        if (player instanceof net.minecraftforge.common.extensions.IForgeEntity forgeEntity) {
            return forgeEntity.getPersistentData();
        }
        return new net.minecraft.nbt.NbtCompound();
    }

    @Override
    public void resetEpicClassStats(Object player) {
        try {
            // Use reflection to avoid mapping issues during compilation
            Class<?> clazz = Class.forName("com.example.epicclassmod.data.PlayerLevelData");
            for (java.lang.reflect.Method m : clazz.getMethods()) {
                if (m.getName().equals("resetAllocatedStatsAndRefund")) {
                    m.invoke(null, player, (Runnable) () -> {
                    });
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void setEpicClassStatPoints(Object player, int amount) {
        try {
            // Get or create ecm_leveling tag directly from Forge persistent data
            net.minecraft.nbt.NbtCompound tag = getPersistentData(player);
            net.minecraft.nbt.NbtCompound leveling;
            if (tag.contains("ecm_leveling")) {
                leveling = tag.getCompound("ecm_leveling");
            } else {
                leveling = new net.minecraft.nbt.NbtCompound();
                tag.put("ecm_leveling", leveling);
            }
            leveling.putInt("stat_points", amount);

            // Trigger sync to client to refresh UI
            net.bluelotuscoding.skillleveling.bridge.forge.EpicClassSyncHelper.forceSync(player);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void syncEpicClassLevel(Object player, int level, int xp, int lastGain) {
        if (player instanceof PlayerEntity sp) {
            net.bluelotuscoding.skillleveling.bridge.forge.EpicClassSyncHelper.syncFromPufferfish(sp, level,
                    xp, lastGain);
        }
    }

    @Override
    public int getPufferfishLevel(Object player,
            net.minecraft.util.Identifier categoryId) {
        return net.puffish.skillsmod.api.SkillsAPI.getCategory(categoryId)
                .flatMap(cat -> cat.getExperience())
                .map(exp -> {
                    Integer level = net.bluelotuscoding.skillleveling.bridge.forge.EpicClassBridgeForgeAccess
                            .invokeExperienceInt(exp, "getLevel", player);
                    return level != null ? level : 0;
                })
                .orElse(0);
    }

    @Override
    public int getPufferfishExperience(Object player,
            net.minecraft.util.Identifier categoryId) {
        return net.puffish.skillsmod.api.SkillsAPI.getCategory(categoryId)
                .flatMap(cat -> cat.getExperience())
                .map(exp -> {
                    Integer xp = net.bluelotuscoding.skillleveling.bridge.forge.EpicClassBridgeForgeAccess
                            .invokeExperienceInt(exp, "getCurrent", player);
                    return xp != null ? xp : 0;
                })
                .orElse(0);
    }

    @Override
    public void addPufferfishExperience(Object player, int amount) {
        // Find any active category for the player and add XP to it
        net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.getActiveCategory(player).ifPresent(catId -> {
            net.puffish.skillsmod.api.SkillsAPI.getCategory(catId).ifPresent(category -> {
                category.getExperience().ifPresent(exp -> {
                    try {
                        // Use reflection to call addTotal(Object player, int amount)
                        java.lang.reflect.Method addTotal = exp.getClass().getMethod("addTotal", Object.class,
                                int.class);
                        addTotal.invoke(exp, player, amount);
                    } catch (Exception ignored) {
                    }
                });
            });
        });
    }

    @Override
    public String getEpicClassName(Object player) {
        if (player instanceof PlayerEntity sp) {
            String custom = net.bluelotuscoding.skillleveling.bridge.data.CustomClassData.getCustomClass(sp);
            if (custom != null && !custom.equals("epic_classes:none")) {
                return custom;
            }
        }
        return net.bluelotuscoding.skillleveling.bridge.forge.EpicClassBridgeForgeAccess
                .getClassName(player);
    }

    @Override
    public int getPufferfishPoints(Object player,
            net.minecraft.util.Identifier categoryId) {
        return net.puffish.skillsmod.api.SkillsAPI.getCategory(categoryId)
                .flatMap(cat -> cat.getExperience())
                .map(exp -> {
                    // Try getTotalPoints first, fallback to getLevel if not found or returns null
                    Integer points = net.bluelotuscoding.skillleveling.bridge.forge.EpicClassBridgeForgeAccess
                            .invokeExperienceInt(exp, "getTotalPoints", player);
                    if (points != null) {
                        return points;
                    }
                    // Fallback to level as a rough estimate if points are not accessible
                    Integer level = net.bluelotuscoding.skillleveling.bridge.forge.EpicClassBridgeForgeAccess
                            .invokeExperienceInt(exp, "getLevel", player);
                    return level != null ? level : 0;
                })
                .orElse(0);
    }

    @Override
    public void syncCustomClassName(Object player) {
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
            String customId = net.bluelotuscoding.skillleveling.bridge.data.CustomClassData.getCustomClass(sp);
            net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new net.bluelotuscoding.skillleveling.bridge.network.CustomSyncClassPacket(sp.getUuid(), customId));
        }
    }

    @Override
    public void setCustomClassName(Object player, String name) {
        if (player instanceof PlayerEntity sp) {
            net.bluelotuscoding.skillleveling.bridge.data.CustomClassData.setCustomClass(sp, name);
        }
    }

    @Override
    public void sendAdvanceClassScreen(Object player, String parentClassId) {
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new net.bluelotuscoding.skillleveling.bridge.network.OpenAdvanceClassScreenPacket(parentClassId));
        }
    }
}
