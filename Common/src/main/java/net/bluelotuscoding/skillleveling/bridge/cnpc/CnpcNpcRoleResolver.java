package net.bluelotuscoding.skillleveling.bridge.cnpc;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

public final class CnpcNpcRoleResolver {
    public static final String FORGE_DATA_KEY = "ForgeData";
    public static final String CNPC_STORED_DATA_KEY = "CNPCStoredData";
    public static final String JOB_MASTER_KEY = "job_master";
    public static final String QUEST_NPC_KEY = "quest_npc";
    public static final String QUEST_MAPPINGS_KEY = "cnpcQuestMappings";

    private CnpcNpcRoleResolver() {
    }

    public static CnpcNpcRoleInfo resolve(Entity entity) {
        if (entity == null) {
            return new CnpcNpcRoleInfo(null, null);
        }

        NbtCompound root = new NbtCompound();
        entity.writeNbt(root);
        return resolve(root);
    }

    public static CnpcNpcRoleInfo resolve(NbtCompound root) {
        if (root == null || !root.contains(FORGE_DATA_KEY, NbtElement.COMPOUND_TYPE)) {
            return new CnpcNpcRoleInfo(null, null);
        }

        NbtCompound forgeData = root.getCompound(FORGE_DATA_KEY);
        if (!forgeData.contains(CNPC_STORED_DATA_KEY, NbtElement.COMPOUND_TYPE)) {
            return new CnpcNpcRoleInfo(null, null);
        }

        NbtCompound storedData = forgeData.getCompound(CNPC_STORED_DATA_KEY);
        return new CnpcNpcRoleInfo(readString(storedData, JOB_MASTER_KEY), readString(storedData, QUEST_NPC_KEY));
    }

    public static String getJobMasterClassId(Entity entity) {
        return resolve(entity).getJobMasterClassId();
    }

    public static String getQuestNpcRoleId(Entity entity) {
        return resolve(entity).getQuestNpcRoleId();
    }

    public static String getStoredString(Entity entity, String key) {
        if (entity == null) {
            return null;
        }
        NbtCompound root = new NbtCompound();
        entity.writeNbt(root);
        return getStoredString(root, key);
    }

    public static String getStoredString(NbtCompound root, String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        NbtCompound storedData = getStoredData(root);
        return storedData == null ? null : readString(storedData, key);
    }

    private static NbtCompound getStoredData(NbtCompound root) {
        if (root == null || !root.contains(FORGE_DATA_KEY, NbtElement.COMPOUND_TYPE)) {
            return null;
        }

        NbtCompound forgeData = root.getCompound(FORGE_DATA_KEY);
        if (!forgeData.contains(CNPC_STORED_DATA_KEY, NbtElement.COMPOUND_TYPE)) {
            return null;
        }
        return forgeData.getCompound(CNPC_STORED_DATA_KEY);
    }

    private static String readString(NbtCompound compound, String key) {
        if (!compound.contains(key, NbtElement.STRING_TYPE)) {
            return null;
        }
        String value = compound.getString(key);
        return value == null || value.isBlank() ? null : value;
    }
}
