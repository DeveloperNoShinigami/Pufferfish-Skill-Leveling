package net.bluelotuscoding.skillleveling.forge.entity;

import net.bluelotuscoding.skillleveling.registry.ModVillagers;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps the custom skill master near its workstation without freezing normal
 * villager behavior. The villager is only nudged home when it strays well
 * outside a sensible radius.
 */
public final class SkillMasterVillagerGuard {
    private static final int SOFT_RADIUS_BLOCKS = 12;
    private static final int HARD_RADIUS_BLOCKS = 28;
    private static final int CHECK_INTERVAL_TICKS = 40;
    private static final double NAVIGATION_SPEED = 0.8D;

    private final Set<UUID> trackedVillagers = new HashSet<>();

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof VillagerEntity villager)) {
            return;
        }
        if (villager.getWorld().isClient()) {
            return;
        }
        if (isSkillMaster(villager)) {
            trackedVillagers.add(villager.getUuid());
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        if (trackedVillagers.isEmpty()) {
            return;
        }

        Set<UUID> stale = new HashSet<>();
        for (ServerWorld world : event.getServer().getWorlds()) {
            List<VillagerEntity> villagers = new ArrayList<>();
            world.iterateEntities().forEach(entity -> {
                if (entity instanceof VillagerEntity villager) {
                    villagers.add(villager);
                }
            });
            for (VillagerEntity villager : villagers) {
                UUID uuid = villager.getUuid();
                if (!trackedVillagers.contains(uuid)) {
                    continue;
                }
                if (villager.isRemoved() || !villager.isAlive()) {
                    stale.add(uuid);
                    continue;
                }
                if (!isSkillMaster(villager)) {
                    stale.add(uuid);
                    continue;
                }
                maybeReturnToWorkstation(villager);
            }
        }

        if (!stale.isEmpty()) {
            trackedVillagers.removeAll(stale);
        }
    }

    private static boolean isSkillMaster(VillagerEntity villager) {
        return villager.getVillagerData().getProfession() == ModVillagers.SKILL_MASTER;
    }

    private static void maybeReturnToWorkstation(VillagerEntity villager) {
        if (villager.age % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        Optional<GlobalPos> memory = villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE);
        if (memory.isEmpty()) {
            return;
        }

        GlobalPos globalPos = memory.get();
        if (!globalPos.getDimension().equals(villager.getWorld().getRegistryKey())) {
            return;
        }

        BlockPos target = globalPos.getPos();
        double sqDistance = villager.squaredDistanceTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        double softRadiusSq = SOFT_RADIUS_BLOCKS * SOFT_RADIUS_BLOCKS;
        double hardRadiusSq = HARD_RADIUS_BLOCKS * HARD_RADIUS_BLOCKS;

        if (sqDistance <= softRadiusSq) {
            return;
        }

        if (sqDistance >= hardRadiusSq) {
            villager.refreshPositionAndAngles(
                    target.getX() + 0.5D,
                    target.getY(),
                    target.getZ() + 0.5D,
                    villager.getYaw(),
                    villager.getPitch());
            villager.getNavigation().stop();
            return;
        }

        villager.getNavigation().startMovingTo(
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                NAVIGATION_SPEED);
    }
}
