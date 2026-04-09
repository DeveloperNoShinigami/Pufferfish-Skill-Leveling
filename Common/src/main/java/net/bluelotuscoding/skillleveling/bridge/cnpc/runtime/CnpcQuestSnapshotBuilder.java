package net.bluelotuscoding.skillleveling.bridge.cnpc.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestDisplayView;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestIntegrationResolver;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestMappingDef;
import net.minecraft.server.network.ServerPlayerEntity;

final class CnpcQuestSnapshotBuilder {
    private final CnpcQuestDefinitionReader definitionReader = new CnpcQuestDefinitionReader();

    Snapshot build(ServerPlayerEntity player, CnpcPlayerQuestReader.PlayerQuestSnapshot state) throws Exception {
        Map<String, CnpcQuestDisplayView> views = new LinkedHashMap<>();

        for (CnpcPlayerQuestReader.QuestRuntimeEntry entry : state.activeQuests()) {
            CnpcQuestDisplayView view = tryBuildView(player, entry);
            if (view != null) {
                views.put(entry.questId(), view);
            }
        }
        for (CnpcPlayerQuestReader.QuestRuntimeEntry entry : state.finishedQuests()) {
            CnpcQuestDisplayView view = tryBuildView(player, entry);
            if (view != null) {
                views.putIfAbsent(entry.questId(), view);
            }
        }

        List<CnpcQuestDisplayView> orderedViews = new ArrayList<>(views.values());
        orderedViews.sort(Comparator
                .comparing((CnpcQuestDisplayView view) -> categorySort(view.bookCategory))
                .thenComparing(view -> view.questId, CnpcQuestSnapshotBuilder::compareQuestIds));

        LinkedHashSet<String> accepted = new LinkedHashSet<>();
        LinkedHashSet<String> readyToTurnIn = new LinkedHashSet<>();
        for (CnpcQuestDisplayView view : orderedViews) {
            if (state.acceptedQuestIds().contains(view.questId)) {
                accepted.add(view.questId);
            }
            if (state.readyToTurnInQuestIds().contains(view.questId)) {
                readyToTurnIn.add(view.questId);
            }
        }

        LinkedHashSet<String> completed = new LinkedHashSet<>();
        for (CnpcQuestDisplayView view : orderedViews) {
            if (state.completedQuestIds().contains(view.questId)) {
                completed.add(view.questId);
            }
        }

        return new Snapshot(orderedViews, List.copyOf(accepted), List.copyOf(readyToTurnIn), List.copyOf(completed));
    }

    private CnpcQuestDisplayView tryBuildView(ServerPlayerEntity player, CnpcPlayerQuestReader.QuestRuntimeEntry entry)
            throws Exception {
        try {
            return buildView(player, entry);
        } catch (Exception e) {
            net.bluelotuscoding.skillleveling.util.AddonLogger.LOGGER.warn(
                    "Skipping CNPC quest "
                            + entry.questId()
                            + " during snapshot build: "
                            + e);
            return null;
        }
    }

    private CnpcQuestDisplayView buildView(ServerPlayerEntity player, CnpcPlayerQuestReader.QuestRuntimeEntry entry)
            throws Exception {
        CnpcQuestDefinitionReader.QuestDefinition definition = definitionReader.read(
                entry.questId(), entry.quest(), player, entry.completed());
        CnpcQuestMappingDef mapping = CnpcQuestIntegrationResolver.getQuestMapping(entry.questId());

        CnpcQuestDisplayView view = new CnpcQuestDisplayView();
        view.questId = definition.questId();
        view.title = fallback(definition.title(), definition.questId());
        view.body = firstNonBlank(definition.body(), definition.completeText(), definition.progressText());
        view.requirementLabel = firstRequirementLabel(definition);
        view.progressText = definition.progressText();
        view.bookCategory = mapping != null && mapping.getNormalizedBookCategory() != null
                ? mapping.getNormalizedBookCategory()
                : "sub";
        view.classId = mapping != null ? normalizeClassId(mapping.classId) : null;
        view.trackStructure = mapping != null ? mapping.trackStructure : null;
        view.completeText = definition.completeText();
        view.completerNpc = definition.completerNpc();
        view.questType = definition.questType();
        view.readyToTurnIn = entry.active() && entry.completed();
        view.requirements.addAll(definition.requirements());
        view.rewards.addAll(definition.rewards());
        return view;
    }

    private static String firstRequirementLabel(CnpcQuestDefinitionReader.QuestDefinition definition) {
        for (var requirement : definition.requirements()) {
            if (requirement.text != null && !requirement.text.isBlank()) {
                return requirement.text;
            }
        }
        return definition.progressText();
    }

    private static String fallback(String primary, String secondary) {
        return firstNonBlank(primary, secondary, "Unknown Quest");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String normalizeClassId(String classId) {
        if (classId == null || classId.isBlank()) {
            return null;
        }
        String normalized = classId.trim().toLowerCase(java.util.Locale.ROOT);
        int colon = normalized.indexOf(':');
        return colon >= 0 ? normalized.substring(colon + 1) : normalized;
    }

    private static int categorySort(String category) {
        if (category == null) {
            return 2;
        }
        return switch (category.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "main" -> 0;
            case "job" -> 1;
            default -> 2;
        };
    }

    private static int compareQuestIds(String left, String right) {
        try {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        } catch (NumberFormatException ignored) {
            return left.compareTo(right);
        }
    }

    record Snapshot(List<CnpcQuestDisplayView> quests, List<String> acceptedQuestIds, List<String> readyToTurnInQuestIds,
            List<String> completedQuestIds) {
    }
}
