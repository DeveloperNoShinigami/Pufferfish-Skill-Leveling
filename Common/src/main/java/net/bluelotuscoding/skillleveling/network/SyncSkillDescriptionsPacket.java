package net.bluelotuscoding.skillleveling.network;

import net.bluelotuscoding.skillleveling.client.ClientDescriptionStorage;
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

    public SyncSkillDescriptionsPacket(String definitionId,
            Map<Integer, String> levelDescriptions,
            Map<Integer, String> levelExtraDescriptions,
            boolean mergeDescription,
            int maxLevel) {
        this.definitionId = definitionId;
        this.levelDescriptions = levelDescriptions != null ? levelDescriptions : new HashMap<>();
        this.levelExtraDescriptions = levelExtraDescriptions != null ? levelExtraDescriptions : new HashMap<>();
        this.mergeDescription = mergeDescription;
        this.maxLevel = maxLevel;
    }

    public static SyncSkillDescriptionsPacket decode(PacketByteBuf buf) {
        String definitionId = buf.readString();
        int maxLevel = buf.readVarInt();
        boolean mergeDescription = buf.readBoolean();

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

        return new SyncSkillDescriptionsPacket(definitionId, levelDescs, extraDescs, mergeDescription, maxLevel);
    }

    public void encode(PacketByteBuf buf) {
        buf.writeString(definitionId);
        buf.writeVarInt(maxLevel);
        buf.writeBoolean(mergeDescription);

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
    }

    /**
     * Handle packet on client side - store descriptions in
     * ClientDescriptionStorage.
     */
    public void handleClient() {
        ClientDescriptionStorage.setDescriptions(
                definitionId,
                levelDescriptions,
                levelExtraDescriptions,
                mergeDescription,
                maxLevel);
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
