package net.bluelotuscoding.skillleveling.bridge.cnpc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.network.SyncCnpcQuestUiPacket;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CnpcQuestSyncManager {
    private static final Map<UUID, CnpcQuestUiSnapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private CnpcQuestSyncManager() {
    }

    public static void updatePlayerState(ServerPlayerEntity player, Collection<CnpcQuestDisplayView> quests,
            Collection<String> accepted, Collection<String> readyToTurnIn, Collection<String> completed) {
        if (player == null) {
            return;
        }
        CnpcQuestUiSnapshot previous = SNAPSHOTS.get(player.getUuid());
        CnpcQuestUiSnapshot snapshot = new CnpcQuestUiSnapshot(
                quests == null ? List.of() : new ArrayList<>(quests),
                accepted == null ? List.of() : new ArrayList<>(accepted),
                readyToTurnIn == null ? List.of() : new ArrayList<>(readyToTurnIn),
                completed == null ? List.of() : new ArrayList<>(completed));
        if (previous != null && previous.sameState(snapshot)) {
            return;
        }
        SNAPSHOTS.put(player.getUuid(), snapshot);
        syncToPlayer(player);
    }

    public static void syncToPlayer(ServerPlayerEntity player) {
        if (player == null || SkillLevelingMod.getInstance().getNetworkHandler() == null) {
            return;
        }
        CnpcQuestUiSnapshot snapshot = SNAPSHOTS.getOrDefault(player.getUuid(), CnpcQuestUiSnapshot.EMPTY);
        SkillLevelingMod.getInstance().getNetworkHandler()
                .sendToPlayer(
                        new SyncCnpcQuestUiPacket(snapshot.quests, snapshot.accepted, snapshot.completed,
                                snapshot.readyToTurnIn),
                        player);
    }

    public static void clearPlayerState(ServerPlayerEntity player) {
        clearPlayerState(player, false);
    }

    public static void clearPlayerState(ServerPlayerEntity player, boolean forceSync) {
        if (player == null) {
            return;
        }
        if (SNAPSHOTS.remove(player.getUuid()) != null || forceSync) {
            syncToPlayer(player);
        }
    }

    private record CnpcQuestUiSnapshot(List<CnpcQuestDisplayView> quests, List<String> accepted,
            List<String> readyToTurnIn, List<String> completed) {
        private static final CnpcQuestUiSnapshot EMPTY = new CnpcQuestUiSnapshot(List.of(), List.of(), List.of(),
                List.of());

        private boolean sameState(CnpcQuestUiSnapshot other) {
            if (other == null || accepted.size() != other.accepted.size()
                    || readyToTurnIn.size() != other.readyToTurnIn.size()
                    || completed.size() != other.completed.size()
                    || quests.size() != other.quests.size()) {
                return false;
            }
            if (!accepted.equals(other.accepted)
                    || !readyToTurnIn.equals(other.readyToTurnIn)
                    || !completed.equals(other.completed)) {
                return false;
            }
            for (int i = 0; i < quests.size(); i++) {
                if (!quests.get(i).toNbt().equals(other.quests.get(i).toNbt())) {
                    return false;
                }
            }
            return true;
        }

    }
}
