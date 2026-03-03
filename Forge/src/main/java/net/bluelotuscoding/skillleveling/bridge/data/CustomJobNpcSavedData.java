package net.bluelotuscoding.skillleveling.bridge.data;

import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomJobNpcSavedData extends PersistentState {
    public static final String ID = "ecm_custom_job_npc_data";

    // Mappings: job_id -> data
    private final Map<String, BlockPos> npcPos = new HashMap<>();
    private final Map<String, UUID> npcUuid = new HashMap<>();
    private final Map<String, Boolean> spawned = new HashMap<>();

    public CustomJobNpcSavedData() {
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        NbtCompound posTag = new NbtCompound();
        npcPos.forEach((id, pos) -> {
            if (pos != null) {
                posTag.put(id, NbtHelper.fromBlockPos(pos));
            }
        });
        tag.put("npc_pos", posTag);

        NbtCompound uuidTag = new NbtCompound();
        npcUuid.forEach((id, u) -> {
            if (u != null) {
                uuidTag.putUuid(id, u);
            }
        });
        tag.put("uuid", uuidTag);

        NbtCompound spawnedTag = new NbtCompound();
        spawned.forEach((id, v) -> spawnedTag.putBoolean(id, v));
        tag.put("spawned", spawnedTag);

        return tag;
    }

    public static CustomJobNpcSavedData load(NbtCompound tag) {
        CustomJobNpcSavedData d = new CustomJobNpcSavedData();

        NbtCompound posTag = tag.getCompound("npc_pos");
        for (String key : posTag.getKeys()) {
            d.npcPos.put(key, NbtHelper.toBlockPos(posTag.getCompound(key)));
        }

        NbtCompound uuidTag = tag.getCompound("uuid");
        for (String key : uuidTag.getKeys()) {
            d.npcUuid.put(key, uuidTag.getUuid(key));
        }

        NbtCompound spawnedTag = tag.getCompound("spawned");
        for (String key : spawnedTag.getKeys()) {
            d.spawned.put(key, spawnedTag.getBoolean(key));
        }

        return d;
    }

    public static CustomJobNpcSavedData get(ServerWorld level) {
        return level.getPersistentStateManager().getOrCreate(
                CustomJobNpcSavedData::load,
                CustomJobNpcSavedData::new,
                ID);
    }

    public BlockPos getNpcPos(String jobId) {
        return npcPos.get(jobId);
    }

    public void setNpcPos(String jobId, BlockPos pos) {
        if (pos == null) {
            npcPos.remove(jobId);
        } else {
            npcPos.put(jobId, pos);
        }
        markDirty();
    }

    public UUID getUuid(String jobId) {
        return npcUuid.get(jobId);
    }

    public void setUuid(String jobId, UUID u) {
        if (u == null) {
            npcUuid.remove(jobId);
        } else {
            npcUuid.put(jobId, u);
        }
        markDirty();
    }

    public boolean isNpcSpawned(String jobId) {
        return Boolean.TRUE.equals(spawned.get(jobId));
    }

    public void setNpcSpawned(String jobId, boolean v) {
        spawned.put(jobId, v);
        markDirty();
    }
}
