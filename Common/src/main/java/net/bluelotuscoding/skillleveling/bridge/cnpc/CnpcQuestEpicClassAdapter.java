package net.bluelotuscoding.skillleveling.bridge.cnpc;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.bluelotuscoding.skillleveling.client.CnpcClientQuestState;
import net.bluelotuscoding.skillleveling.client.ClientCustomClassState;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class CnpcQuestEpicClassAdapter {
    private static final ThreadLocal<Map<String, Integer>> OVERLAY_LINE_COLORS = ThreadLocal
            .withInitial(LinkedHashMap::new);

    private static Class<?> questDefClass;
    private static Class<?> questRewardClass;
    private static Constructor<?> questDefComponentCtor;
    private static Constructor<?> questRewardCtor;
    private static Method questRewardXpMethod;
    private static Method questDefReqMethod;

    private CnpcQuestEpicClassAdapter() {
    }

    public static Object[] getMainQuestDefs() {
        return toQuestDefArray(CnpcClientQuestState.getQuestsForBookCategory("main", null), false);
    }

    public static Object[] getSideQuestDefs() {
        return toQuestDefArray(CnpcClientQuestState.getQuestsForBookCategory("sub", null), false);
    }

    public static Object[] getJobQuestDefs(String currentClassId) {
        return toQuestDefArray(CnpcClientQuestState.getQuestsForBookCategory("job", currentClassId), false);
    }

    public static Object resolveQuestDef(String key) {
        CnpcQuestDisplayView view = CnpcClientQuestState.getQuest(key);
        if (view == null) {
            return null;
        }
        return toQuestDef(view, false);
    }

    public static Object resolveOverlayQuestDef(String key) {
        CnpcQuestDisplayView view = CnpcClientQuestState.getQuest(key);
        if (view == null) {
            return null;
        }
        return toQuestDef(view, true);
    }

    public static String getCurrentClientClassId() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return null;
        }
        String customId = ClientCustomClassState.getCustomClass(client.player.getUuid());
        if (customId != null && !customId.isBlank() && !"epic_classes:none".equals(customId)) {
            return normalizeClassId(customId);
        }
        return null;
    }

    public static EpicClassDef getCurrentClientClassDef() {
        String classId = getCurrentClientClassId();
        if (classId == null || classId.isBlank()) {
            return null;
        }
        return EpicClassConfigManager.getClassDef(classId);
    }

    public static String getCurrentClientClassLabelKey() {
        EpicClassDef def = getCurrentClientClassDef();
        if (def == null) {
            return null;
        }
        if (def.display_name != null && !def.display_name.isBlank()) {
            return def.display_name;
        }
        return null;
    }

    public static String getCurrentClientClassLabel() {
        EpicClassDef def = getCurrentClientClassDef();
        if (def == null) {
            return null;
        }
        if (def.display_name != null && !def.display_name.isBlank()) {
            return def.display_name;
        }
        if (def.display_name_key != null && !def.display_name_key.isBlank() && I18n.hasTranslation(def.display_name_key)) {
            return I18n.translate(def.display_name_key);
        }
        if (def.gui_title != null && !def.gui_title.isBlank()) {
            return I18n.hasTranslation(def.gui_title) ? I18n.translate(def.gui_title) : def.gui_title;
        }
        if (def.class_name != null && !def.class_name.isBlank()) {
            return humanizeClassId(def.class_name);
        }
        return null;
    }

    public static boolean isMirroredQuestKey(String questKey) {
        return CnpcClientQuestState.getQuest(questKey) != null;
    }

    public static boolean isReadyToTurnIn(String questKey) {
        CnpcQuestDisplayView view = CnpcClientQuestState.getQuest(questKey);
        return view != null && view.readyToTurnIn;
    }

    public static boolean hasTextOnlyRequirements(String questKey) {
        CnpcQuestDisplayView view = CnpcClientQuestState.getQuest(questKey);
        if (view == null || view.requirements.isEmpty()) {
            return false;
        }
        for (CnpcQuestDisplayRequirement requirement : view.requirements) {
            if (requirement != null && requirement.stack != null && !requirement.stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static void clearOverlayRenderContext() {
        OVERLAY_LINE_COLORS.remove();
    }

    public static int resolveOverlayLineColor(String line, int fallbackColor) {
        if (line == null || line.isBlank()) {
            return fallbackColor;
        }
        Integer color = OVERLAY_LINE_COLORS.get().get(line);
        return color == null ? fallbackColor : color;
    }

    public static String resolveQuestKey(Object questDef) {
        if (questDef == null) {
            return null;
        }
        String metaId = readStringField(questDef, "metaId");
        if (metaId != null && !metaId.isBlank()) {
            return metaId;
        }
        try {
            Object key = questDef.getClass().getMethod("key").invoke(questDef);
            return key instanceof String stringKey && !stringKey.isBlank() ? stringKey : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object[] toQuestDefArray(List<CnpcQuestDisplayView> quests, boolean overlay) {
        ensureReflection();
        Object array = Array.newInstance(questDefClass, quests.size());
        for (int i = 0; i < quests.size(); i++) {
            Array.set(array, i, toQuestDef(quests.get(i), overlay));
        }
        return (Object[]) array;
    }

    private static Object toQuestDef(CnpcQuestDisplayView view, boolean overlay) {
        ensureReflection();
        try {
            List<Object> rewards = new ArrayList<>();
            for (CnpcQuestDisplayReward reward : view.rewards) {
                Object questReward = toQuestReward(reward);
                if (questReward != null) {
                    rewards.add(questReward);
                }
            }
            Object rewardsArray = Array.newInstance(questRewardClass, rewards.size());
            for (int i = 0; i < rewards.size(); i++) {
                Array.set(rewardsArray, i, rewards.get(i));
            }

            List<DisplayRequirement> displayRequirements = buildDisplayRequirements(view, overlay);
            String title = fallback(view.title, view.questId);
            String body = overlay
                    ? fallback(resolveOverlayBody(view), "-")
                    : fallback(firstNonBlank(view.body, view.progressText), "-");
            String requireLabel = overlay ? ""
                    : fallback(resolveBookRequirementText(view), "");

            Object questDef = questDefComponentCtor.newInstance(
                    view.questId,
                    null,
                    null,
                    Text.literal(title),
                    Text.literal(body),
                    Text.literal(requireLabel),
                    rewardsArray);

            if (!displayRequirements.isEmpty()) {
                List<ItemStack> stacks = new ArrayList<>();
                for (DisplayRequirement requirement : displayRequirements) {
                    if (requirement.stack == null || requirement.stack.isEmpty()) {
                        continue;
                    }
                    ItemStack copy = requirement.stack.copy();
                    int target = requirement.target > 0 ? requirement.target : Math.max(1, copy.getCount());
                    copy.setCount(Math.max(1, target));
                    stacks.add(copy);
                }
                if (!stacks.isEmpty()) {
                    Object stackArray = Array.newInstance(ItemStack.class, stacks.size());
                    for (int i = 0; i < stacks.size(); i++) {
                        Array.set(stackArray, i, stacks.get(i));
                    }
                    questDefReqMethod.invoke(questDef, stackArray);
                }
            }
            if (overlay) {
                registerOverlayLine(view, body, displayRequirements.isEmpty());
            }

            try {
                questDefClass.getField("metaId").set(questDef, view.questId);
            } catch (Exception ignored) {
            }
            return questDef;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build Epic Class QuestDef for CNPC quest " + view.questId, e);
        }
    }

    private static List<DisplayRequirement> buildDisplayRequirements(CnpcQuestDisplayView view, boolean overlay) {
        List<DisplayRequirement> out = new ArrayList<>();

        for (CnpcQuestDisplayRequirement requirement : view.requirements) {
            if (requirement == null) {
                continue;
            }
            String normalizedText = normalizeRequirementText(view.questType, requirement.text);
            if (requirement.stack != null && !requirement.stack.isEmpty()) {
                ItemStack stack = requirement.stack.copy();
                if (normalizedText != null && !normalizedText.isBlank()) {
                    stack.setCustomName(Text.literal(normalizedText));
                }
                out.add(new DisplayRequirement(stack, requirement.current, requirement.target,
                        normalizedText));
            }
        }
        return out;
    }

    private static String normalizedRequirementLabel(CnpcQuestDisplayView view) {
        if (view == null) {
            return "";
        }
        for (CnpcQuestDisplayRequirement requirement : view.requirements) {
            String normalized = normalizeRequirementText(view.questType, requirement == null ? null : requirement.text);
            if (normalized != null && !normalized.isBlank()) {
                return normalized;
            }
        }
        return firstNonBlank(view.requirementLabel, view.progressText, "");
    }

    private static String resolveBookRequirementText(CnpcQuestDisplayView view) {
        return firstNonBlank(normalizedRequirementLabel(view), view.progressText, view.requirementLabel, "");
    }

    private static String resolveOverlayBody(CnpcQuestDisplayView view) {
        if (view == null) {
            return "";
        }
        if (hasRealStackRequirements(view)) {
            return firstNonBlank(view.progressText, view.body, "-");
        }
        return switch (view.questType) {
            case 3, 1, 5 -> firstNonBlank(normalizedRequirementLabel(view), view.progressText, view.body, "-");
            default -> firstNonBlank(view.progressText, normalizedRequirementLabel(view), view.body, "-");
        };
    }

    private static String normalizeRequirementText(int questType, String text) {
        String normalized = firstNonBlank(text, "");
        if (normalized.isBlank()) {
            return "";
        }
        return switch (questType) {
            case 3 -> normalizeLocationRequirement(normalized);
            case 1 -> normalizeDialogRequirement(normalized);
            default -> normalized;
        };
    }

    private static String normalizeLocationRequirement(String text) {
        String normalized = text.trim();
        normalized = normalized.replaceAll("(?i)\\(?\\s*found\\s*\\)?", "")
                .replaceAll("(?i)\\(?\\s*not\\s*found\\s*\\)?", "")
                .trim();
        normalized = normalized.replaceAll("(?i)not\\s*found", "").trim();
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.toLowerCase(java.util.Locale.ROOT).startsWith("find ")) {
            return normalized;
        }
        if (normalized.endsWith("...")) {
            return "Find " + normalized;
        }
        return "Find " + normalized + "...";
    }

    private static String normalizeDialogRequirement(String text) {
        String normalized = text.trim();
        normalized = normalized.replace("(read)", "")
                .replace("(unread)", "")
                .replace("(Read)", "")
                .replace("(Unread)", "")
                .trim();
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.toLowerCase(java.util.Locale.ROOT).startsWith("speak ")) {
            return normalized;
        }
        return "Speak To " + normalized;
    }

    private static boolean hasRealStackRequirements(CnpcQuestDisplayView view) {
        for (CnpcQuestDisplayRequirement requirement : view.requirements) {
            if (requirement != null && requirement.stack != null && !requirement.stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void registerOverlayLine(CnpcQuestDisplayView view, String body, boolean textOnly) {
        if (!textOnly || body == null || body.isBlank()) {
            return;
        }
        String line = "\u2022 " + ellipsize(body, 36);
        OVERLAY_LINE_COLORS.get().put(line, view.readyToTurnIn ? -9375888 : -38294);
    }

    private static Object toQuestReward(CnpcQuestDisplayReward reward) throws Exception {
        if (reward.kind == CnpcQuestDisplayReward.Kind.XP) {
            return questRewardXpMethod.invoke(null, reward.xp);
        }
        if (reward.stack == null || reward.stack.isEmpty()) {
            return null;
        }
        return questRewardCtor.newInstance(reward.stack.copy());
    }

    private static void ensureReflection() {
        if (questDefClass != null) {
            return;
        }
        try {
            questDefClass = Class.forName("com.example.epicclassmod.data.quest.QuestDef");
            questRewardClass = Class.forName("com.example.epicclassmod.data.quest.QuestReward");
            questDefComponentCtor = questDefClass.getConstructor(String.class, String.class, String.class,
                    Text.class, Text.class, Text.class, Array.newInstance(questRewardClass, 0).getClass());
            questRewardCtor = questRewardClass.getConstructor(ItemStack.class);
            questRewardXpMethod = questRewardClass.getMethod("xp", int.class);
            questDefReqMethod = questDefClass.getMethod("req", Array.newInstance(ItemStack.class, 0).getClass());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Epic Class quest reflection adapter", e);
        }
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
        return "";
    }

    private static String normalizeClassId(String classId) {
        String normalized = classId.trim().toLowerCase(java.util.Locale.ROOT);
        int colon = normalized.indexOf(':');
        return colon >= 0 ? normalized.substring(colon + 1) : normalized;
    }

    private static String humanizeClassId(String classId) {
        String normalized = normalizeClassId(classId).replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isBlank()) {
            return "";
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private static String readStringField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getField(fieldName);
            Object value = field.get(target);
            return value == null ? null : value.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String ellipsize(String value, int max) {
        if (value == null) {
            return "-";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 1)) + "\u2026";
    }

    private record DisplayRequirement(ItemStack stack, int current, int target, String text) {
    }
}
