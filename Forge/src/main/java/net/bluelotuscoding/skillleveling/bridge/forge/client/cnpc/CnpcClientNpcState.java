package net.bluelotuscoding.skillleveling.bridge.forge.client.cnpc;

import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcNpcRoleInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CnpcClientNpcState {
    private static final Map<Integer, CnpcNpcRoleInfo> ROLES = new ConcurrentHashMap<>();

    private CnpcClientNpcState() {
    }

    public static void put(int entityId, String jobMasterClassId, String questNpcRoleId) {
        if (entityId < 0) {
            return;
        }
        CnpcNpcRoleInfo info = new CnpcNpcRoleInfo(jobMasterClassId, questNpcRoleId);
        if (info.hasAnyRole()) {
            ROLES.put(entityId, info);
        } else {
            ROLES.remove(entityId);
        }
    }

    public static CnpcNpcRoleInfo get(int entityId) {
        return ROLES.get(entityId);
    }

    public static boolean isMirrored(int entityId) {
        CnpcNpcRoleInfo info = ROLES.get(entityId);
        return info != null && info.hasAnyRole();
    }

    public static void clear() {
        ROLES.clear();
    }
}
