package net.bluelotuscoding.skillleveling.bridge.cnpc;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.Collection;

public final class CnpcBridgeHooks {
    private CnpcBridgeHooks() {
    }

    public static CnpcNpcRoleInfo getNpcRole(net.minecraft.entity.Entity entity) {
        return CnpcNpcRoleResolver.resolve(entity);
    }

    public static String getJobMasterClassId(net.minecraft.entity.Entity entity) {
        return CnpcNpcRoleResolver.getJobMasterClassId(entity);
    }

    public static String getQuestNpcRoleId(net.minecraft.entity.Entity entity) {
        return CnpcNpcRoleResolver.getQuestNpcRoleId(entity);
    }

    public static void openClassSelection(ServerPlayerEntity player, String classId) {
        if (player == null || classId == null || classId.isBlank()) {
            return;
        }
        if (EpicClassConfigManager.getClassDef(classId) == null) {
            AddonLogger.LOGGER.warn("CNPC bridge ignored class selection request for unknown class: " + classId);
            return;
        }
        SkillLevelingMod.getInstance().getPlatform().sendAdvanceClassScreen(player, classId);
    }

    /**
     * Returns true if the player has mastered all skills in their current class
     * category AND at least one child class is available to advance into.
     * Callable from CNPC scripts via Java.type().
     */
    public static boolean isReadyToAdvance(ServerPlayerEntity player) {
        if (player == null) return false;
        var platform = SkillLevelingMod.getInstance().getPlatform();
        String currentClass = platform.getEpicClassName(player);
        if (currentClass == null || currentClass.isEmpty()
                || "epic_classes:none".equalsIgnoreCase(currentClass)) return false;
        if (EpicClassConfigManager.getChildClasses(currentClass).isEmpty()) return false;

        var def = EpicClassConfigManager.getClassDef(currentClass);
        if (def == null || def.skill_category_id == null || def.skill_category_id.isEmpty()) return true;

        var categoryId = net.bluelotuscoding.skillleveling.bridge.EpicClassBridge.resolveCategoryId(def.skill_category_id);
        if (categoryId == null) return false;

        var categoryOpt = net.puffish.skillsmod.api.SkillsAPI.getCategory(categoryId);
        if (categoryOpt.isEmpty() || !categoryOpt.get().isUnlocked(player)) return false;

        int neededExp = SkillLevelingMod.getInstance().getPlatform()
                .getPufferfishNeededExperience(player, categoryId);
        return neededExp <= 0;
    }

    public static void syncBridge(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        SkillLevelingMod.getInstance().syncBridgeContent(player);
        SkillLevelingMod.getInstance().getSkillLevelingManager().syncAllSkillsToPlayer(player);
    }

    public static void notifyQuestMilestone(ServerPlayerEntity player, String questId) {
        notifyQuestMilestone(player, questId, null);
    }

    public static void notifyQuestMilestone(ServerPlayerEntity player, String questId, String fallbackClassId) {
        if (player == null || questId == null || questId.isBlank()) {
            return;
        }

        String resolvedClassId = fallbackClassId;
        if (resolvedClassId == null || resolvedClassId.isBlank()) {
            resolvedClassId = CnpcQuestIntegrationResolver.resolveClassId(questId, null);
        }

        if (resolvedClassId != null && !resolvedClassId.isBlank()) {
            openClassSelection(player, resolvedClassId);
        }

        syncBridge(player);
    }

    public static void notifyAdvancementQuest(ServerPlayerEntity player, String questId, String classId) {
        notifyQuestMilestone(player, questId, classId);
    }

    public static void syncQuestUi(ServerPlayerEntity player, Collection<CnpcQuestDisplayView> quests,
            Collection<String> accepted, Collection<String> completed) {
        CnpcQuestSyncManager.updatePlayerState(player, quests, accepted, java.util.List.of(), completed);
    }

    public static void clearQuestUi(ServerPlayerEntity player) {
        CnpcQuestSyncManager.clearPlayerState(player);
    }
}
