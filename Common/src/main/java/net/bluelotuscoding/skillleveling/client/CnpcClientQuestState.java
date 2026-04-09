package net.bluelotuscoding.skillleveling.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.bluelotuscoding.skillleveling.bridge.BridgeConfigManager;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestDisplayView;

public final class CnpcClientQuestState {
    private static final Map<String, CnpcQuestDisplayView> QUESTS = new LinkedHashMap<>();
    private static final LinkedHashSet<String> ACCEPTED = new LinkedHashSet<>();
    private static final LinkedHashSet<String> READY_TO_TURN_IN = new LinkedHashSet<>();
    private static final LinkedHashSet<String> COMPLETED = new LinkedHashSet<>();

    private CnpcClientQuestState() {
    }

    public static boolean isEnabled() {
        return BridgeConfigManager.getConfig() != null && BridgeConfigManager.getConfig().useCnpcQuests;
    }

    public static void replaceState(Collection<CnpcQuestDisplayView> quests, Collection<String> accepted,
            Collection<String> readyToTurnIn, Collection<String> completed) {
        QUESTS.clear();
        ACCEPTED.clear();
        READY_TO_TURN_IN.clear();
        COMPLETED.clear();

        if (quests != null) {
            for (CnpcQuestDisplayView view : quests) {
                if (view == null || view.questId == null || view.questId.isBlank()) {
                    continue;
                }
                QUESTS.put(normalizeKey(view.questId), view);
            }
        }
        if (accepted != null) {
            accepted.stream().map(CnpcClientQuestState::normalizeKey).filter(s -> !s.isBlank()).forEach(ACCEPTED::add);
        }
        if (readyToTurnIn != null) {
            readyToTurnIn.stream().map(CnpcClientQuestState::normalizeKey).filter(s -> !s.isBlank())
                    .forEach(READY_TO_TURN_IN::add);
        }
        if (completed != null) {
            completed.stream().map(CnpcClientQuestState::normalizeKey).filter(s -> !s.isBlank()).forEach(COMPLETED::add);
        }
    }

    public static Set<String> getAcceptedKeys() {
        return Collections.unmodifiableSet(ACCEPTED);
    }

    public static Set<String> getCompletedKeys() {
        return Collections.unmodifiableSet(COMPLETED);
    }

    public static Set<String> getReadyToTurnInKeys() {
        return Collections.unmodifiableSet(READY_TO_TURN_IN);
    }

    public static boolean isAccepted(String key) {
        return key != null && ACCEPTED.contains(normalizeKey(key));
    }

    public static boolean isCompleted(String key) {
        return key != null && COMPLETED.contains(normalizeKey(key));
    }

    public static boolean isReadyToTurnIn(String key) {
        return key != null && READY_TO_TURN_IN.contains(normalizeKey(key));
    }

    public static boolean isAnyQuestWithTrackStructureCompleted(String trackStructure) {
        if (trackStructure == null || trackStructure.isBlank()) {
            return false;
        }
        for (CnpcQuestDisplayView view : QUESTS.values()) {
            if (trackStructure.equalsIgnoreCase(view.trackStructure)
                    && COMPLETED.contains(normalizeKey(view.questId))) {
                return true;
            }
        }
        return false;
    }

    public static CnpcQuestDisplayView getQuest(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = normalizeKey(key);
        CnpcQuestDisplayView direct = QUESTS.get(normalized);
        if (direct != null) {
            return direct;
        }
        if (normalized.endsWith(".title")) {
            return QUESTS.get(normalized.substring(0, normalized.length() - 6));
        }
        return QUESTS.get(normalized + ".title");
    }

    public static List<CnpcQuestDisplayView> getQuestsForBookCategory(String category, String classId) {
        List<CnpcQuestDisplayView> out = new ArrayList<>();
        String normalizedCategory = normalizeCategory(category);
        String normalizedClass = normalizeClassId(classId);
        for (String acceptedKey : ACCEPTED) {
            CnpcQuestDisplayView view = getQuest(acceptedKey);
            if (view == null) {
                continue;
            }
            if (!normalizedCategory.equals(normalizeCategory(view.bookCategory))) {
                continue;
            }
            if ("job".equals(normalizedCategory) && normalizedClass != null) {
                String questClass = normalizeClassId(view.classId);
                if (questClass != null && !normalizedClass.equals(questClass)) {
                    continue;
                }
            }
            out.add(view);
        }
        return out;
    }

    public static List<CnpcQuestDisplayView> getAllAcceptedQuestViews() {
        List<CnpcQuestDisplayView> out = new ArrayList<>();
        for (String acceptedKey : ACCEPTED) {
            CnpcQuestDisplayView view = getQuest(acceptedKey);
            if (view != null) {
                out.add(view);
            }
        }
        return out;
    }

    public static void removeAccepted(String key) {
        if (key == null) {
            return;
        }
        String normalized = normalizeKey(key);
        ACCEPTED.remove(normalized);
        READY_TO_TURN_IN.remove(normalized);
        QUESTS.remove(normalized);
    }

    public static void clear() {
        QUESTS.clear();
        ACCEPTED.clear();
        READY_TO_TURN_IN.clear();
        COMPLETED.clear();
    }

    private static String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "sub";
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        if ("side".equals(normalized)) {
            return "sub";
        }
        return normalized;
    }

    private static String normalizeClassId(String classId) {
        if (classId == null || classId.isBlank()) {
            return null;
        }
        String normalized = classId.trim().toLowerCase(Locale.ROOT);
        int colon = normalized.indexOf(':');
        return colon >= 0 ? normalized.substring(colon + 1) : normalized;
    }
}
