package net.bluelotuscoding.skillleveling.bridge.cnpc.runtime;

import java.lang.reflect.InvocationTargetException;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcNpcRoleResolver;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestIntegrationResolver;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestStoredMappingIndex;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestSyncManager;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CnpcRuntimeBridge {
    private static final CnpcPlayerQuestReader PLAYER_READER = new CnpcPlayerQuestReader();
    private static final CnpcQuestSnapshotBuilder SNAPSHOT_BUILDER = new CnpcQuestSnapshotBuilder();

    private CnpcRuntimeBridge() {
    }

    public static void dropAndRefresh(ServerPlayerEntity player, int questId) {
        if (player == null) {
            return;
        }
        try {
            CnpcReflectionAccess.dropActiveQuest(player, questId);
        } catch (Exception e) {
            AddonLogger.LOGGER.warn("Failed to drop CNPC active quest " + questId + " for "
                    + player.getEntityName() + ": " + e);
        }
        refreshPlayer(player);
    }

    public static void refreshPlayer(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        if (!CnpcQuestIntegrationResolver.isCnpcQuestModeEnabled() || !CnpcReflectionAccess.isAvailable()) {
            CnpcQuestSyncManager.clearPlayerState(player, true);
            return;
        }
        try {
            CnpcQuestStoredMappingIndex.refresh(player.getServer());
            var state = PLAYER_READER.read(player);
            var snapshot = SNAPSHOT_BUILDER.build(player, state);
            CnpcQuestSyncManager.updatePlayerState(player, snapshot.quests(), snapshot.acceptedQuestIds(),
                    snapshot.readyToTurnInQuestIds(), snapshot.completedQuestIds());
        } catch (Exception e) {
            Throwable root = unwrap(e);
            AddonLogger.LOGGER.warn("Failed to refresh CNPC quest state for " + player.getEntityName() + ": " + root);
            CnpcQuestSyncManager.clearPlayerState(player, true);
        }
    }

    public static void refreshIfRelevantInteraction(ServerPlayerEntity player, Entity entity) {
        if (player == null || entity == null || !CnpcQuestIntegrationResolver.isCnpcQuestModeEnabled()) {
            return;
        }
        if (!CnpcNpcRoleResolver.resolve(entity).hasAnyRole()) {
            return;
        }
        var server = player.getServer();
        if (server != null) {
            server.execute(() -> refreshPlayer(player));
        } else {
            refreshPlayer(player);
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof InvocationTargetException invocation && invocation.getCause() != null) {
            current = invocation.getCause();
        }
        return current;
    }
}
