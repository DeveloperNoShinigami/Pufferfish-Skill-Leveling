package net.bluelotuscoding.skillleveling.bridge.cnpc.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.network.ServerPlayerEntity;

final class CnpcPlayerQuestReader {

    PlayerQuestSnapshot read(ServerPlayerEntity player) throws Exception {
        Object playerData = CnpcReflectionAccess.getPlayerData(player);
        Map<Object, Object> active = CnpcReflectionAccess.getActiveQuestData(playerData);
        Map<Object, Object> finished = CnpcReflectionAccess.getFinishedQuestData(playerData);

        List<QuestRuntimeEntry> activeEntries = new ArrayList<>();
        for (Object questData : active.values()) {
            Object quest = CnpcReflectionAccess.getQuestFromQuestData(questData);
            if (quest == null) {
                continue;
            }
            activeEntries.add(new QuestRuntimeEntry(
                    CnpcReflectionAccess.getQuestId(quest),
                    quest,
                    CnpcReflectionAccess.isQuestDataCompleted(questData),
                    true));
        }

        List<QuestRuntimeEntry> finishedEntries = new ArrayList<>();
        for (Object key : finished.keySet()) {
            int questId = key instanceof Number number ? number.intValue() : parseQuestId(key);
            if (questId < 0) {
                continue;
            }
            Object quest = CnpcReflectionAccess.getQuestById(questId);
            if (quest == null) {
                continue;
            }
            finishedEntries.add(new QuestRuntimeEntry(Integer.toString(questId), quest, true, false));
        }

        activeEntries.sort(Comparator.comparing(QuestRuntimeEntry::questId, CnpcPlayerQuestReader::compareQuestIds));
        finishedEntries.sort(Comparator.comparing(QuestRuntimeEntry::questId, CnpcPlayerQuestReader::compareQuestIds));

        LinkedHashSet<String> acceptedIds = new LinkedHashSet<>();
        LinkedHashSet<String> readyToTurnInIds = new LinkedHashSet<>();
        for (QuestRuntimeEntry entry : activeEntries) {
            acceptedIds.add(entry.questId());
            if (entry.completed()) {
                readyToTurnInIds.add(entry.questId());
            }
        }

        LinkedHashSet<String> completedIds = new LinkedHashSet<>();
        for (QuestRuntimeEntry entry : finishedEntries) {
            completedIds.add(entry.questId());
        }

        return new PlayerQuestSnapshot(activeEntries, finishedEntries, acceptedIds, readyToTurnInIds, completedIds);
    }

    private static int parseQuestId(Object key) {
        if (key == null) {
            return -1;
        }
        try {
            return Integer.parseInt(key.toString());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static int compareQuestIds(String left, String right) {
        try {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        } catch (NumberFormatException ignored) {
            return left.compareTo(right);
        }
    }

    record QuestRuntimeEntry(String questId, Object quest, boolean completed, boolean active) {
    }

    record PlayerQuestSnapshot(List<QuestRuntimeEntry> activeQuests, List<QuestRuntimeEntry> finishedQuests,
            Set<String> acceptedQuestIds, Set<String> readyToTurnInQuestIds, Set<String> completedQuestIds) {
    }
}
