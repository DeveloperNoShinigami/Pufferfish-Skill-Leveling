package net.bluelotuscoding.skillleveling.bridge.cnpc;

import net.bluelotuscoding.skillleveling.bridge.BridgeConfig;
import net.bluelotuscoding.skillleveling.bridge.BridgeConfigManager;

public final class CnpcQuestIntegrationResolver {
    private CnpcQuestIntegrationResolver() {
    }

    public static boolean isCnpcQuestModeEnabled() {
        BridgeConfig config = BridgeConfigManager.getConfig();
        return config != null && config.useCnpcQuests;
    }

    public static CnpcQuestMappingDef getQuestMapping(String questId) {
        BridgeConfig config = BridgeConfigManager.getConfig();
        if (questId == null || questId.isBlank()) {
            return null;
        }

        if (config != null && config.cnpcQuestMappings != null) {
            CnpcQuestMappingDef exact = config.cnpcQuestMappings.get(questId);
            if (exact != null) {
                return exact;
            }

            CnpcQuestMappingDef normalized = config.cnpcQuestMappings
                    .get(questId.trim().toLowerCase(java.util.Locale.ROOT));
            if (normalized != null) {
                return normalized;
            }
        }

        return CnpcQuestStoredMappingIndex.getQuestMapping(questId);
    }

    public static String resolveClassId(String questId, CnpcNpcRoleInfo npcRole) {
        CnpcQuestMappingDef mapping = getQuestMapping(questId);
        if (mapping != null && mapping.classId != null && !mapping.classId.isBlank()) {
            return mapping.classId.trim();
        }
        return npcRole == null ? null : npcRole.getJobMasterClassId();
    }

    public static String resolveBookCategory(String questId) {
        CnpcQuestMappingDef mapping = getQuestMapping(questId);
        return mapping == null ? null : mapping.getNormalizedBookCategory();
    }
}
