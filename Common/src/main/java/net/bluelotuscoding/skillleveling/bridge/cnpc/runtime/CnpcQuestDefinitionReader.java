package net.bluelotuscoding.skillleveling.bridge.cnpc.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestDisplayRequirement;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestDisplayReward;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

final class CnpcQuestDefinitionReader {

    QuestDefinition read(String questId, Object quest, ServerPlayerEntity player, boolean completed) throws Exception {
        List<CnpcQuestDisplayRequirement> requirements = new ArrayList<>();
        List<String> progressLines = new ArrayList<>();

        if (!completed) {
            for (Object objective : CnpcReflectionAccess.getObjectives(quest, player)) {
                int progress = CnpcReflectionAccess.getObjectiveProgress(objective);
                int target = CnpcReflectionAccess.getObjectiveMaxProgress(objective);
                String text = CnpcReflectionAccess.getObjectiveText(objective);
                ItemStack stack = CnpcReflectionAccess.getObjectiveStack(objective);
                requirements.add(new CnpcQuestDisplayRequirement(stack, progress, target, text));
                if (text != null && !text.isBlank()) {
                    progressLines.add(text);
                }
            }
        }

        List<CnpcQuestDisplayReward> rewards = new ArrayList<>();
        Collection<?> rewardStacks = CnpcReflectionAccess.getRewardStacks(quest);
        for (Object value : rewardStacks) {
            if (value instanceof ItemStack stack && !stack.isEmpty()) {
                rewards.add(CnpcQuestDisplayReward.item(stack));
            }
        }
        int rewardXp = CnpcReflectionAccess.getQuestRewardExp(quest);
        if (rewardXp > 0) {
            rewards.add(CnpcQuestDisplayReward.xp(rewardXp));
        }

        String completeText = trim(CnpcReflectionAccess.getQuestCompleteText(quest));
        String progressText = progressLines.isEmpty() ? null : String.join(" | ", progressLines);
        if (completed) {
            requirements.clear();
            if (progressText == null || progressText.isBlank()) {
                progressText = firstNonBlank(completeText, "Completed");
            }
        }

        return new QuestDefinition(
                questId,
                trim(CnpcReflectionAccess.getQuestTitle(quest)),
                trim(CnpcReflectionAccess.getQuestLogText(quest)),
                trim(CnpcReflectionAccess.getQuestCompleterNpc(quest)),
                completeText,
                progressText,
                requirements,
                rewards,
                CnpcReflectionAccess.getQuestType(quest));
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    record QuestDefinition(String questId, String title, String body, String completerNpc, String completeText,
            String progressText, List<CnpcQuestDisplayRequirement> requirements, List<CnpcQuestDisplayReward> rewards,
            int questType) {
    }
}
