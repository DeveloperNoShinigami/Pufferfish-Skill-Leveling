package net.bluelotuscoding.skillleveling.bridge.forge.event;

import com.example.epicclassmod.EpicClassMod;
import com.example.epicclassmod.init.ModEntities;
import com.example.epicclassmod.world.entity.NpcQuestGiverEntity;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef;
import net.bluelotuscoding.skillleveling.bridge.config.JobMasterDef;
import net.bluelotuscoding.skillleveling.bridge.data.CustomJobNpcSavedData;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = "puffish_skill_leveling", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CustomJobMasterSpawner {
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int SCAN_RADIUS_XZ = 32;
    private static final int SCAN_RADIUS_Y = 16;

    /**
     * Fires once the server is fully started and the world's ResourceManager is
     * ready.
     * Loads the Pufferfish skill title/description cache from definitions.json so
     * the
     * class selection screen can display them without needing client-side API
     * access.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        EpicClassBridge.loadSkillDisplayData(event.getServer().getResourceManager());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        EpicClassBridge.invalidateSkillDisplayCache();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayerEntity sp)) {
            return;
        }

        ServerWorld level = (ServerWorld) sp.getWorld();
        if ((level.getTime() + sp.getId()) % SCAN_INTERVAL_TICKS != 0) {
            return;
        }

        CustomJobNpcSavedData data = CustomJobNpcSavedData.get(level);
        Collection<JobMasterDef> jobMasters = EpicClassConfigManager.getJobMasters().values();

        for (JobMasterDef def : jobMasters) {
            if (def.marker_block == null) {
                continue;
            }

            // We use the job master ID for persistence
            if (data.isNpcSpawned(def.id)) {
                continue;
            }

            handleCustomJobMaster(level, sp, data, def);
        }
    }

    private static void handleCustomJobMaster(ServerWorld level, ServerPlayerEntity sp, CustomJobNpcSavedData data,
            JobMasterDef def) {
        Block markerBlock = ForgeRegistries.BLOCKS.getValue(new Identifier(def.marker_block));
        if (markerBlock == null || markerBlock == Blocks.AIR) {
            return;
        }

        BlockPos center = sp.getBlockPos();
        int minY = Math.max(level.getBottomY(), center.getY() - SCAN_RADIUS_Y);
        int maxY = Math.min(level.getTopY() - 1, center.getY() + SCAN_RADIUS_Y);

        for (int x = center.getX() - SCAN_RADIUS_XZ; x <= center.getX() + SCAN_RADIUS_XZ; x++) {
            for (int z = center.getZ() - SCAN_RADIUS_XZ; z <= center.getZ() + SCAN_RADIUS_XZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(p);
                    if (state.isOf(markerBlock)) {
                        spawnCustomJobMaster(level, p.up(), data, def);
                        return;
                    }
                }
            }
        }
    }

    private static void spawnCustomJobMaster(ServerWorld level, BlockPos spawnPos, CustomJobNpcSavedData data,
            JobMasterDef def) {
        NpcQuestGiverEntity npc = ModEntities.NPC_QUEST_GIVER.get().create(level);
        if (npc == null) {
            return;
        }

        // Use the job master ID for mapping back to our config
        npc.setNpcId(def.id);
        npc.setPersistent();

        double x = spawnPos.getX() + 0.5;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5;
        npc.refreshPositionAndAngles(x, y, z, 0.0f, 0.0f);

        String name = def.name_key != null ? def.name_key : "npc.epicclassmod.job_master.generic";
        npc.setCustomName(Text.translatable(name).formatted(Formatting.GOLD));
        npc.setCustomNameVisible(true);

        level.spawnEntity(npc);

        data.setNpcPos(def.id, spawnPos);
        data.setNpcSpawned(def.id, true);
        data.setUuid(def.id, npc.getUuid());

        EpicClassMod.LOGGER.info("[ECM-Bridge] Custom Job Master '{}' spawned at {}", def.id, spawnPos);
    }
}
