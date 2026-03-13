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
    public void syncEpicClassLevel(Object player, int level, int xp, int neededXp, int lastGain) {
        if (player instanceof PlayerEntity sp) {
            net.bluelotuscoding.skillleveling.bridge.forge.EpicClassSyncHelper.syncFromPufferfish(sp, level,
                    xp, neededXp, lastGain);
        }
    }

    @Override
    public int getPufferfishLevel(Object player,
            net.minecraft.util.Identifier categoryId) {
        int level = getLevelInternal(player, categoryId);
        if (level > 0) {
            return level;
        }

        // Namespace fallback: if level is 0, check categories with the same path but
        // DIFFERENT namespaces
        String path = categoryId.getPath();
        for (net.puffish.skillsmod.api.Category cat : net.puffish.skillsmod.api.SkillsAPI.streamCategories().toList()) {
            if (cat.getId().getPath().equals(path) && !cat.getId().getNamespace().equals(categoryId.getNamespace())) {
                int altLevel = getLevelInternal(player, cat.getId());
                if (altLevel > 0) {
                    return altLevel;
                }
            }
        }

        return 0;
    }

    private int getLevelInternal(Object player, net.minecraft.util.Identifier categoryId) {
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
        int xp = getExperienceInternal(player, categoryId);
        if (xp > 0) {
            return xp;
        }

        // Namespace fallback
        String path = categoryId.getPath();
        for (net.puffish.skillsmod.api.Category cat : net.puffish.skillsmod.api.SkillsAPI.streamCategories().toList()) {
            if (cat.getId().getPath().equals(path) && !cat.getId().getNamespace().equals(categoryId.getNamespace())) {
                int altXp = getExperienceInternal(player, cat.getId());
                if (altXp > 0) {
                    return altXp;
                }
            }
        }

        return 0;
    }

    private int getExperienceInternal(Object player, net.minecraft.util.Identifier categoryId) {
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
    public int getPufferfishNeededExperience(Object player,
            net.minecraft.util.Identifier categoryId) {
        return net.puffish.skillsmod.api.SkillsAPI.getCategory(categoryId)
                .flatMap(cat -> cat.getExperience())
                .map(exp -> {
                    // Use the official API to get the requirement for the current level
                    Integer level = net.bluelotuscoding.skillleveling.bridge.forge.EpicClassBridgeForgeAccess
                            .invokeExperienceInt(exp, "getLevel", player);
                    if (level != null) {
                        try {
                            java.lang.reflect.Method getRequired = exp.getClass().getMethod("getRequired", int.class);
                            getRequired.setAccessible(true);
                            return (Integer) getRequired.invoke(exp, level.intValue());
                        } catch (Exception e) {
                            // Fallback to the previous reflection if getRequired(int) fails
                            Integer needed = net.bluelotuscoding.skillleveling.bridge.forge.EpicClassBridgeForgeAccess
                                    .invokeExperienceInt(exp, "getNeeded", player);
                            if (needed == null) {
                                needed = net.bluelotuscoding.skillleveling.bridge.forge.EpicClassBridgeForgeAccess
                                        .invokeExperienceInt(exp, "getMax", player);
                            }
                            return needed != null ? needed : 100;
                        }
                    }
                    return 100;
                })
                .orElse(100);
    }

    @Override
    public void addPufferfishExperience(Object player, int amount) {
        // Find any active category for the player and add XP to it
        net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.getActiveCategory(player).ifPresent(catId -> {
            addPufferfishExperience(player, catId, amount);
        });
    }

    @Override
    public void addPufferfishExperience(Object player, net.minecraft.util.Identifier categoryId, int amount) {
        net.puffish.skillsmod.api.SkillsAPI.getCategory(categoryId).ifPresent(category -> {
            category.getExperience().ifPresent(exp -> {
                try {
                    java.lang.reflect.Method method = null;
                    for (java.lang.reflect.Method m : exp.getClass().getMethods()) {
                        if (m.getParameterCount() == 2 && m.getParameterTypes()[1] == int.class) {
                            if (m.getParameterTypes()[0].isAssignableFrom(player.getClass())) {
                                String name = m.getName();
                                if (name.equals("addTotal") || name.equals("addExperience")) {
                                    method = m;
                                    break;
                                }
                            }
                        }
                    }
                    if (method == null) {
                        for (java.lang.reflect.Method m : exp.getClass().getMethods()) {
                            if (m.getParameterCount() == 2 && m.getParameterTypes()[1] == int.class) {
                                if (m.getParameterTypes()[0].isAssignableFrom(player.getClass())) {
                                    String name = m.getName().toLowerCase();
                                    if (name.contains("experience") || name.contains("add")) {
                                        method = m;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (method == null) {
                        // DEEP DEBUG LOGGING
                        StringBuilder debug = new StringBuilder("Failed to find XP gain method. Available methods:\n");
                        for (java.lang.reflect.Method m : exp.getClass().getMethods()) {
                            debug.append(" - ").append(m.getName()).append("(");
                            Class<?>[] params = m.getParameterTypes();
                            for (int i = 0; i < params.length; i++) {
                                debug.append(params[i].getName());
                                if (i < params.length - 1) {
                                    debug.append(", ");
                                }
                            }
                            debug.append(")\n");
                        }
                        debug.append("Player class: ").append(player.getClass().getName());
                        net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                                .error(debug.toString());
                    }

                    if (method != null) {
                        method.setAccessible(true);
                        method.invoke(exp, player, amount);
                    } else {
                        net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                                .error("Failed to find XP gain method on " + exp.getClass().getName());
                    }
                } catch (Exception e) {
                    net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                            .error("Error invoking XP gain method on " + exp.getClass().getName() + ": "
                                    + e.getMessage());
                }
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

    @Override
    public boolean isClient() {
        return net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient();
    }

    @Override
    public void cleanupPlayerData(Object player) {
        if (player instanceof PlayerEntity sp) {
            net.bluelotuscoding.skillleveling.bridge.forge.EpicClassSyncHelper.cleanupData(sp);
        }
    }

    @Override
    public void cleanupWorldData(Object level) {
        if (level instanceof net.minecraft.server.world.ServerWorld sw) {
            net.bluelotuscoding.skillleveling.bridge.data.CustomJobNpcSavedData data = net.bluelotuscoding.skillleveling.bridge.data.CustomJobNpcSavedData
                    .get(sw);
            data.clearAll();
        }
    }
}
