package net.bluelotuscoding.skillleveling.client;

import net.minecraft.util.Identifier;
import java.util.Map;
import java.util.List;
import net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward.SkillPrerequisite;

/**
 * Safely handles client-side packet logic to avoid NoClassDefFoundError on
 * integrated servers.
 */
public class ClientPacketHandler {

    public static void handleSyncSkillLevel(Identifier categoryId, String skillId, int baseLevel, int totalLevel,
            int maxLevel, int pointsPerLevel, String definitionId, boolean hidden, boolean toggle, int keybindSlot,
            boolean active, String lootMode) {
        ClientSkillLevelStorage.setLevel(categoryId.toString(), skillId, baseLevel, totalLevel, maxLevel,
                pointsPerLevel, hidden, toggle, keybindSlot, active, lootMode);
        if (definitionId != null) {
            ClientSkillLevelStorage.registerDefinitionMapping(definitionId, categoryId.toString(), skillId);
        }
    }

    public static void handleSyncDescriptions(String definitionId, Map<Integer, String> levelDescriptions,
            Map<Integer, String> levelExtraDescriptions, boolean mergeDescription, int maxLevel, String lootMode,
            List<SkillPrerequisite> prerequisites) {
        ClientDescriptionStorage.setDescriptions(definitionId, levelDescriptions, levelExtraDescriptions,
                mergeDescription, maxLevel, lootMode, prerequisites);
    }

    public static void handleCloseScreen() {
        SideSafeClient.closeScreen();
    }

    public static void handleSyncToggleCooldown(Identifier categoryId, String skillId, int cooldownTicks) {
        ClientSkillLevelStorage.setCooldown(categoryId.toString(), skillId, cooldownTicks);
    }
}
