package net.bluelotuscoding.skillleveling.bridge.cnpc.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

final class CnpcReflectionAccess {
    private static final String PLAYER_DATA_CLASS = "noppes.npcs.controllers.data.PlayerData";
    private static final String QUEST_CONTROLLER_CLASS = "noppes.npcs.controllers.QuestController";

    private static boolean initialized;
    private static boolean available;

    private static Method playerDataGetMethod;
    private static Method questInterfaceGetObjectivesMethod;
    private static Method objectiveGetProgressMethod;
    private static Method objectiveGetMaxProgressMethod;
    private static Method objectiveGetMcTextMethod;

    private static Field playerDataQuestDataField;
    private static Field questDataActiveQuestsField;
    private static Field questDataFinishedQuestsField;
    private static Field questDataQuestField;
    private static Field questDataCompletedField;
    private static Field questControllerInstanceField;
    private static Field questControllerQuestsField;
    private static Field questQuestInterfaceField;

    // Triggers CNPC's ServerTickHandler to re-sync player data + call VisibilityController.onUpdate()
    private static Field playerDataUpdateClientField;

    // Dialog-related fields (for clearing dialog history on quest abandon)
    private static Field dialogControllerInstanceField;
    private static Field dialogControllerDialogsField;
    private static Field dialogQuestField;
    private static Field playerDataDialogDataField;
    private static Field playerDialogDataDialogsReadField;
    private static Field questIdField;
    private static Field questTitleField;
    private static Field questLogTextField;
    private static Field questCompleteTextField;
    private static Field questCompleterNpcField;
    private static Field questRewardExpField;
    private static Field questRewardItemsField;
    private static Field questTypeField;
    private static Field rewardItemsItemsField;

    private CnpcReflectionAccess() {
    }

    static boolean isAvailable() {
        ensureInitialized();
        return available;
    }

    static Object getPlayerData(ServerPlayerEntity player) throws Exception {
        ensureAvailable();
        return playerDataGetMethod.invoke(null, player);
    }

    @SuppressWarnings("unchecked")
    static Map<Object, Object> getActiveQuestData(Object playerData) throws Exception {
        Object questData = playerDataQuestDataField.get(playerData);
        Object value = questDataActiveQuestsField.get(questData);
        return value instanceof Map<?, ?> map ? (Map<Object, Object>) map : Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    static Map<Object, Object> getFinishedQuestData(Object playerData) throws Exception {
        Object questData = playerDataQuestDataField.get(playerData);
        Object value = questDataFinishedQuestsField.get(questData);
        return value instanceof Map<?, ?> map ? (Map<Object, Object>) map : Collections.emptyMap();
    }

    static Object getQuestFromQuestData(Object questData) throws Exception {
        return questDataQuestField.get(questData);
    }

    static boolean isQuestDataCompleted(Object questData) throws Exception {
        return questDataCompletedField.getBoolean(questData);
    }

    static Object getQuestById(int questId) throws Exception {
        ensureAvailable();
        Object controller = questControllerInstanceField.get(null);
        if (controller == null) {
            return null;
        }
        Object quests = questControllerQuestsField.get(controller);
        if (quests instanceof Map<?, ?> map) {
            return map.get(questId);
        }
        return null;
    }

    static String getQuestId(Object quest) throws Exception {
        return Integer.toString(questIdField.getInt(quest));
    }

    static String getQuestTitle(Object quest) throws Exception {
        return (String) questTitleField.get(quest);
    }

    static String getQuestLogText(Object quest) throws Exception {
        return (String) questLogTextField.get(quest);
    }

    static String getQuestCompleteText(Object quest) throws Exception {
        return (String) questCompleteTextField.get(quest);
    }

    static String getQuestCompleterNpc(Object quest) throws Exception {
        return (String) questCompleterNpcField.get(quest);
    }

    static int getQuestRewardExp(Object quest) throws Exception {
        return questRewardExpField.getInt(quest);
    }

    static int getQuestType(Object quest) throws Exception {
        return questTypeField.getInt(quest);
    }

    static Object[] getObjectives(Object quest, ServerPlayerEntity player) throws Exception {
        ensureAvailable();
        Object questInterface = questQuestInterfaceField.get(quest);
        if (questInterface == null) {
            return new Object[0];
        }
        Object result = questInterfaceGetObjectivesMethod.invoke(questInterface, player);
        return result instanceof Object[] arr ? arr : new Object[0];
    }

    static int getObjectiveProgress(Object objective) throws Exception {
        return ((Number) objectiveGetProgressMethod.invoke(objective)).intValue();
    }

    static int getObjectiveMaxProgress(Object objective) throws Exception {
        return ((Number) objectiveGetMaxProgressMethod.invoke(objective)).intValue();
    }

    static String getObjectiveText(Object objective) throws Exception {
        Object component = objectiveGetMcTextMethod.invoke(objective);
        if (component == null) {
            return "";
        }
        Method getString = findMethod(component.getClass(), "getString", 0);
        if (getString != null) {
            Object result = getString.invoke(component);
            return result == null ? "" : result.toString();
        }
        return component.toString();
    }

    static ItemStack getObjectiveStack(Object objective) {
        try {
            Field field = findField(objective.getClass(), "questItem");
            if (field == null) {
                return ItemStack.EMPTY;
            }
            Object value = field.get(objective);
            return value instanceof ItemStack stack ? stack.copy() : ItemStack.EMPTY;
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    static void dropActiveQuest(ServerPlayerEntity player, int questId) throws Exception {
        Object playerData = getPlayerData(player);
        Map<Object, Object> activeQuests = getActiveQuestData(playerData);
        activeQuests.remove(questId);
        // Clear dialog history for this quest's linked dialogs so the NPC can re-offer it.
        clearDialogsForQuest(playerData, questId);
        // Set updateClient=true so CNPC's ServerTickHandler re-syncs player data to client
        // AND calls VisibilityController.onUpdate() — which refreshes NPC dialog availability.
        if (playerDataUpdateClientField != null) {
            playerDataUpdateClientField.setBoolean(playerData, true);
        }
    }

    @SuppressWarnings("unchecked")
    private static void clearDialogsForQuest(Object playerData, int questId) {
        try {
            if (dialogControllerInstanceField == null || dialogControllerDialogsField == null
                    || dialogQuestField == null || playerDataDialogDataField == null
                    || playerDialogDataDialogsReadField == null) {
                return;
            }
            Object controller = dialogControllerInstanceField.get(null);
            if (controller == null) return;
            Object dialogsObj = dialogControllerDialogsField.get(controller);
            if (!(dialogsObj instanceof Map<?, ?> allDialogs)) return;
            Object dialogData = playerDataDialogDataField.get(playerData);
            if (dialogData == null) return;
            Object dialogsReadObj = playerDialogDataDialogsReadField.get(dialogData);
            if (!(dialogsReadObj instanceof java.util.Set<?> set)) return;
            java.util.Set<Integer> dialogsRead = (java.util.Set<Integer>) set;
            for (Object dialog : allDialogs.values()) {
                int linkedQuest = dialogQuestField.getInt(dialog);
                if (linkedQuest == questId) {
                    Field idField = findField(dialog.getClass(), "id");
                    if (idField != null) {
                        dialogsRead.remove(idField.getInt(dialog));
                    }
                }
            }
        } catch (Exception e) {
            AddonLogger.LOGGER.warn("Failed to clear dialog history for quest " + questId + ": " + e);
        }
    }

    static Collection<?> getRewardStacks(Object quest) throws Exception {
        Object rewardInventory = questRewardItemsField.get(quest);
        if (rewardInventory == null) {
            return List.of();
        }
        Object value = rewardItemsItemsField.get(rewardInventory);
        return value instanceof Collection<?> collection ? collection : List.of();
    }

    private static void ensureAvailable() {
        ensureInitialized();
        if (!available) {
            throw new IllegalStateException("CustomNPCs runtime bridge is not available");
        }
    }

    private static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Class<?> playerDataClass = Class.forName(PLAYER_DATA_CLASS);
            Class<?> questControllerClass = Class.forName(QUEST_CONTROLLER_CLASS);
            Class<?> questClass = Class.forName("noppes.npcs.controllers.data.Quest");
            Class<?> questDataClass = Class.forName("noppes.npcs.controllers.data.QuestData");
            Class<?> playerQuestDataClass = Class.forName("noppes.npcs.controllers.data.PlayerQuestData");
            Class<?> miscInventoryClass = Class.forName("noppes.npcs.NpcMiscInventory");
            Class<?> questInterfaceClass = Class.forName("noppes.npcs.quests.QuestInterface");

            playerDataGetMethod = findMethod(playerDataClass, "get", 1);
            questInterfaceGetObjectivesMethod = findMethod(questInterfaceClass, "getObjectives", 1);

            playerDataQuestDataField = requireField(playerDataClass, "questData");
            questDataActiveQuestsField = requireField(playerQuestDataClass, "activeQuests");
            questDataFinishedQuestsField = requireField(playerQuestDataClass, "finishedQuests");
            questDataQuestField = requireField(questDataClass, "quest");
            questDataCompletedField = requireField(questDataClass, "isCompleted");
            questControllerInstanceField = requireField(questControllerClass, "instance");
            questControllerQuestsField = requireField(questControllerClass, "quests");
            questQuestInterfaceField = requireField(questClass, "questInterface");
            questIdField = requireField(questClass, "id");
            questTitleField = requireField(questClass, "title");
            questLogTextField = requireField(questClass, "logText");
            questCompleteTextField = requireField(questClass, "completeText");
            questCompleterNpcField = requireField(questClass, "completerNpc");
            questRewardExpField = requireField(questClass, "rewardExp");
            questRewardItemsField = requireField(questClass, "rewardItems");
            questTypeField = requireField(questClass, "type");
            rewardItemsItemsField = requireField(miscInventoryClass, "items");

            // updateClient field — best-effort
            playerDataUpdateClientField = findField(playerDataClass, "updateClient");

            // Dialog fields (best-effort — don't fail init if missing)
            try {
                Class<?> dialogControllerClass = Class.forName("noppes.npcs.controllers.DialogController");
                Class<?> dialogClass = Class.forName("noppes.npcs.controllers.data.Dialog");
                Class<?> playerDialogDataClass = Class.forName("noppes.npcs.controllers.data.PlayerDialogData");
                dialogControllerInstanceField = requireField(dialogControllerClass, "instance");
                dialogControllerDialogsField = requireField(dialogControllerClass, "dialogs");
                dialogQuestField = requireField(dialogClass, "quest");
                playerDataDialogDataField = requireField(playerDataClass, "dialogData");
                playerDialogDataDialogsReadField = requireField(playerDialogDataClass, "dialogsRead");
            } catch (Exception e) {
                AddonLogger.LOGGER.warn("Dialog reflection bridge unavailable (quest abandon won't clear dialogs): " + e);
            }

            Class<?> objectiveClass = Class.forName("noppes.npcs.api.handler.data.IQuestObjective");
            objectiveGetProgressMethod = requireMethod(objectiveClass, "getProgress", 0);
            objectiveGetMaxProgressMethod = requireMethod(objectiveClass, "getMaxProgress", 0);
            objectiveGetMcTextMethod = requireMethod(objectiveClass, "getMCText", 0);

            available = playerDataGetMethod != null
                    && questInterfaceGetObjectivesMethod != null;
        } catch (Exception e) {
            available = false;
            AddonLogger.LOGGER.warn("Failed to initialize CustomNPCs runtime bridge: " + e);
        }
    }

    private static Method requireMethod(Class<?> type, String name, int paramCount) {
        Method method = findMethod(type, name, paramCount);
        if (method == null) {
            throw new IllegalStateException("Missing method " + type.getName() + "#" + name);
        }
        method.setAccessible(true);
        return method;
    }

    private static Field requireField(Class<?> type, String name) {
        Field field = findField(type, name);
        if (field == null) {
            throw new IllegalStateException("Missing field " + type.getName() + "#" + name);
        }
        field.setAccessible(true);
        return field;
    }

    private static Method findMethod(Class<?> type, String name, int paramCount) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == paramCount) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == paramCount) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
