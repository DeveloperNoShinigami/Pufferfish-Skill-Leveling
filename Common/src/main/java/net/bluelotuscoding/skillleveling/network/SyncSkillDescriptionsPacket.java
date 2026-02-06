package net.bluelotuscoding.skillleveling.network;

import net.minecraft.network.PacketByteBuf;

import java.util.HashMap;
import java.util.Map;

/**
 * Syncs per-level descriptions from server to client for a skill definition.
 * Sent on first unlock to provide the client with all level description data.
 */
public class SyncSkillDescriptionsPacket {

    private final String definitionId;
    private final Map<Integer, String> levelDescriptions;
    private final Map<Integer, String> levelExtraDescriptions;
    private final boolean mergeDescription;
    private final int maxLevel;
    private final String lootMode;
    private final java.util.List<net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward.SkillPrerequisite> prerequisites;

    public SyncSkillDescriptionsPacket(String definitionId,
            Map<Integer, String> levelDescriptions,
            Map<Integer, String> levelExtraDescriptions,
            boolean mergeDescription,
            int maxLevel,
            String lootMode,
            java.util.List<net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward.SkillPrerequisite> prerequisites) {
        this.definitionId = definitionId;
        this.levelDescriptions = levelDescriptions != null ? levelDescriptions : new HashMap<>();
        this.levelExtraDescriptions = levelExtraDescriptions != null ? levelExtraDescriptions : new HashMap<>();
        this.mergeDescription = mergeDescription;
        this.maxLevel = maxLevel;
        this.lootMode = lootMode;
        this.prerequisites = prerequisites;
    }

    public static SyncSkillDescriptionsPacket decode(PacketByteBuf buf) {
        String definitionId = buf.readString();
        int maxLevel = buf.readVarInt();
        boolean mergeDescription = buf.readBoolean();
        String lootMode = buf.readString();

        // Read level descriptions map
        int descCount = buf.readVarInt();
        Map<Integer, String> levelDescs = new HashMap<>();
        for (int i = 0; i < descCount; i++) {
            int level = buf.readVarInt();
            String text = buf.readString();
            levelDescs.put(level, text);
        }

        // Read extra descriptions map
        int extraCount = buf.readVarInt();
        Map<Integer, String> extraDescs = new HashMap<>();
        for (int i = 0; i < extraCount; i++) {
            int level = buf.readVarInt();
            String text = buf.readString();
            extraDescs.put(level, text);
        }

        // Read prerequisites
        int prereqCount = buf.readVarInt();
        java.util.List<net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward.SkillPrerequisite> prereqs = new java.util.ArrayList<>();
        for (int i = 0; i < prereqCount; i++) {
            String psId = buf.readString();
            int pLevel = buf.readVarInt();
            String pCatId = buf.readBoolean() ? buf.readString() : null;
            prereqs.add(new net.bluelotuscoding.skillleveling.rewards.PerLevelRewardsReward.SkillPrerequisite(psId,
                    pLevel, pCatId));
        }

        return new SyncSkillDescriptionsPacket(definitionId, levelDescs, extraDescs, mergeDescription, maxLevel,
                lootMode, prereqs);
    }

    public void encode(PacketByteBuf buf) {
        buf.writeString(definitionId);
        buf.writeVarInt(maxLevel);
        buf.writeBoolean(mergeDescription);
        buf.writeString(lootMode != null ? lootMode : "");

        // Write level descriptions
        buf.writeVarInt(levelDescriptions.size());
        for (var entry : levelDescriptions.entrySet()) {
            buf.writeVarInt(entry.getKey());
            buf.writeString(entry.getValue());
        }

        // Write extra descriptions
        buf.writeVarInt(levelExtraDescriptions.size());
        for (var entry : levelExtraDescriptions.entrySet()) {
            buf.writeVarInt(entry.getKey());
            buf.writeString(entry.getValue());
        }

        // Write prerequisites
        if (prerequisites != null) {
            buf.writeVarInt(prerequisites.size());
            for (var prereq : prerequisites) {
                buf.writeString(prereq.getSkillId());
                buf.writeVarInt(prereq.getLevel());
                buf.writeBoolean(prereq.getCategoryId() != null);
                if (prereq.getCategoryId() != null) {
                    buf.writeString(prereq.getCategoryId());
                }
            }
        } else {
            buf.writeVarInt(0);
        }
    }

    /**
     * Handle packet on client side - store descriptions in
     * ClientDescriptionStorage.
     */
    public void handleClient() {
        net.bluelotuscoding.skillleveling.client.ClientPacketHandler.handleSyncDescriptions(definitionId,
                levelDescriptions,
                levelExtraDescriptions, mergeDescription, maxLevel, lootMode, prerequisites);
    }

    // Getters
    public String getDefinitionId() {
        return definitionId;
    }

    public Map<Integer, String> getLevelDescriptions() {
        return levelDescriptions;
    }

    public Map<Integer, String> getLevelExtraDescriptions() {
        return levelExtraDescriptions;
    }

    public boolean isMergeDescription() {
        return mergeDescription;
    }

    public int getMaxLevel() {
        return maxLevel;
    }
}
