package net.bluelotuscoding.skillleveling.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

/**
 * NETWORK PROTOCOL FOR SKILL LEVEL SYNCHRONIZATION
 * 
 * PURPOSE: Handles client-server communication for skill level data, ensuring
 * client-side UI elements stay synchronized with server-side progression data.
 * Manages efficient data transmission and update notifications.
 * 
 * ARCHITECTURE: Implements packet-based communication to send skill level
 * updates, description changes, and progression notifications between server
 * and client for seamless UI integration.
 */
public class SkillLevelingNetwork {

    // ================================================
    // NETWORK PACKET IDENTIFIERS
    // ================================================

    /**
     * PACKET TYPES: Network packet identifiers for different sync operations
     * 
     * PACKET DESIGN: Each packet type handles specific synchronization needs
     * to minimize network traffic and ensure efficient data transmission.
     */
    public static final Identifier SKILL_LEVEL_UPDATE = SkillLevelingMod.createIdentifier("skill_level_update");
    public static final Identifier SKILL_DESCRIPTION_UPDATE = SkillLevelingMod
            .createIdentifier("skill_description_update");
    public static final Identifier SKILL_PROGRESSION_UPDATE = SkillLevelingMod
            .createIdentifier("skill_progression_update");
    public static final Identifier FULL_SKILL_SYNC = SkillLevelingMod.createIdentifier("full_skill_sync");
    public static final Identifier TOGGLE_COOLDOWN = SkillLevelingMod.createIdentifier("toggle_cooldown");
    public static final Identifier REQUEST_TOGGLE_SKILL = SkillLevelingMod.createIdentifier("request_toggle_skill");
    public static final Identifier CLOSE_SKILL_SCREEN = SkillLevelingMod.createIdentifier("close_skill_screen");

    // ================================================
    // SERVER-TO-CLIENT SYNCHRONIZATION
    // ================================================

    /**
     * LEVEL UPDATE PACKET: Syncs individual skill level changes
     * 
     * UPDATE MECHANICS: Sends single skill level updates when advancement
     * or refunding occurs. Efficient for real-time progression updates
     * without full data resynchronization.
     */
    public static class SkillLevelUpdatePacket {
        private final Identifier categoryId;
        private final String skillId;
        private final int currentLevel;
        private final int maxLevel;

        public SkillLevelUpdatePacket(Identifier categoryId, String skillId, int currentLevel, int maxLevel) {
            this.categoryId = categoryId;
            this.skillId = skillId;
            this.currentLevel = currentLevel;
            this.maxLevel = maxLevel;
        }

        /**
         * PACKET ENCODING: Writes packet data to network buffer
         * 
         * ENCODING MECHANICS: Serializes skill level data into compact
         * binary format for efficient network transmission.
         */
        public void write(PacketByteBuf buf) {
            buf.writeIdentifier(categoryId);
            buf.writeString(skillId);
            buf.writeVarInt(currentLevel);
            buf.writeVarInt(maxLevel);
        }

        /**
         * PACKET DECODING: Reads packet data from network buffer
         * 
         * DECODING MECHANICS: Deserializes binary data back into skill
         * level information for client-side processing.
         */
        public static SkillLevelUpdatePacket read(PacketByteBuf buf) {
            Identifier categoryId = buf.readIdentifier();
            String skillId = buf.readString();
            int currentLevel = buf.readVarInt();
            int maxLevel = buf.readVarInt();

            return new SkillLevelUpdatePacket(categoryId, skillId, currentLevel, maxLevel);
        }

        // Getters for client-side processing
        public Identifier getCategoryId() {
            return categoryId;
        }

        public String getSkillId() {
            return skillId;
        }

        public int getCurrentLevel() {
            return currentLevel;
        }

        public int getMaxLevel() {
            return maxLevel;
        }
    }

    /**
     * DESCRIPTION UPDATE PACKET: Syncs skill description changes
     * 
     * DESCRIPTION MECHANICS: Sends description and merge configuration updates
     * when skill definitions change or when client needs current tooltip data.
     */
    public static class SkillDescriptionUpdatePacket {
        private final Identifier categoryId;
        private final String skillId;
        private final java.util.Map<Integer, String> descriptions;
        private final java.util.Map<Integer, String> extraDescriptions;
        private final boolean mergeDescription;

        public SkillDescriptionUpdatePacket(Identifier categoryId, String skillId,
                java.util.Map<Integer, String> descriptions,
                java.util.Map<Integer, String> extraDescriptions,
                boolean mergeDescription) {
            this.categoryId = categoryId;
            this.skillId = skillId;
            this.descriptions = descriptions;
            this.extraDescriptions = extraDescriptions;
            this.mergeDescription = mergeDescription;
        }

        /**
         * DESCRIPTION ENCODING: Writes description data to network buffer
         * 
         * ENCODING MECHANICS: Serializes description maps and merge settings
         * for efficient network transmission of tooltip data.
         */
        public void write(PacketByteBuf buf) {
            buf.writeIdentifier(categoryId);
            buf.writeString(skillId);
            buf.writeBoolean(mergeDescription);

            // DESCRIPTION MAP ENCODING: Serialize level-to-description mappings
            buf.writeVarInt(descriptions.size());
            descriptions.forEach((level, desc) -> {
                buf.writeVarInt(level);
                buf.writeString(desc);
            });

            buf.writeVarInt(extraDescriptions.size());
            extraDescriptions.forEach((level, desc) -> {
                buf.writeVarInt(level);
                buf.writeString(desc);
            });
        }

        /**
         * DESCRIPTION DECODING: Reads description data from network buffer
         * 
         * DECODING MECHANICS: Deserializes description maps and merge settings
         * for client-side tooltip and UI processing.
         */
        public static SkillDescriptionUpdatePacket read(PacketByteBuf buf) {
            Identifier categoryId = buf.readIdentifier();
            String skillId = buf.readString();
            boolean mergeDescription = buf.readBoolean();

            // DESCRIPTION MAP DECODING: Deserialize level-to-description mappings
            int descCount = buf.readVarInt();
            java.util.Map<Integer, String> descriptions = new java.util.HashMap<>();
            for (int i = 0; i < descCount; i++) {
                int level = buf.readVarInt();
                String desc = buf.readString();
                descriptions.put(level, desc);
            }

            int extraDescCount = buf.readVarInt();
            java.util.Map<Integer, String> extraDescriptions = new java.util.HashMap<>();
            for (int i = 0; i < extraDescCount; i++) {
                int level = buf.readVarInt();
                String desc = buf.readString();
                extraDescriptions.put(level, desc);
            }

            return new SkillDescriptionUpdatePacket(categoryId, skillId, descriptions, extraDescriptions,
                    mergeDescription);
        }

        // Getters for client-side processing
        public Identifier getCategoryId() {
            return categoryId;
        }

        public String getSkillId() {
            return skillId;
        }

        public java.util.Map<Integer, String> getDescriptions() {
            return descriptions;
        }

        public java.util.Map<Integer, String> getExtraDescriptions() {
            return extraDescriptions;
        }

        public boolean isMergeDescription() {
            return mergeDescription;
        }
    }

    // ================================================
    // SYNCHRONIZATION HELPERS
    // ================================================

    /**
     * PLAYER SYNC: Sends current skill data to specific player
     * 
     * SYNC MECHANICS: Transmits all relevant skill level and description data
     * to connecting player or when full synchronization is needed.
     */
    public static void syncPlayerSkillData(ServerPlayerEntity player) {
        var addon = SkillLevelingMod.getInstance();
        var manager = addon.getSkillLevelingManager();

        // SKILL ITERATION: Send data for all skills with level progression
        net.puffish.skillsmod.api.SkillsAPI.streamCategories().forEach(category -> {
            category.streamSkills().forEach(skill -> {
                // SYNC REGULAR SKILLS (only if they have data)
                boolean hasData = manager.hasSkillData(player, category.getId(), skill.getId());

                // CRITICAL: Also sync skills that are TOGGLES, even if they have no data (level
                // 0)
                // This ensures the client knows they are toggleable and registers keybinds.
                // LeveledConfigStorage handles fuzzy matching for the skill ID.
                var leveledConfig = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage
                        .get(skill.getId().toString());

                if (hasData || (leveledConfig != null && (leveledConfig.toggle || leveledConfig.lootMode != null))) {
                    int baseLevel = manager.getBaseSkillLevel(player, category.getId(), skill.getId());
                    int totalLevel = manager.getTotalSkillLevel(player, category.getId(), skill.getId());
                    int maxLevel = manager.getMaxLevel(category.getId(), skill.getId());

                    // REAL SYNC: Use the manager's sync method which sends the SyncSkillLevelPacket
                    String definitionId = manager.getDefinitionId(category.getId(), skill.getId());
                    manager.syncSkillLevelToClient(player, category.getId(), skill.getId(), baseLevel, totalLevel,
                            maxLevel, definitionId);

                    // DESCRIPTION DATA SYNC: Send description information
                    sendSkillDescriptionUpdate(player, category.getId(), skill.getId());
                }
            });
        });
    }

    /**
     * LEVEL UPDATE TRANSMISSION: Sends single skill level update
     * NOTE: This is now mostly for logging; real sync happens via
     * SkillLevelingManager.syncSkillLevelToClient
     */
    public static void sendSkillLevelUpdate(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int currentLevel, int maxLevel) {
    }

    /**
     * DESCRIPTION UPDATE TRANSMISSION: Sends skill description data
     */
    public static void sendSkillDescriptionUpdate(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        var addon = SkillLevelingMod.getInstance();
        var manager = addon.getSkillLevelingManager();
        var networkHandler = addon.getNetworkHandler();

        if (networkHandler == null)
            return;

        // Find definitionId
        String definitionId = manager.getDefinitionId(categoryId, skillId);

        // DESCRIPTION COLLECTION: Gather all description data for skill
        var descriptions = manager.getDescriptions(categoryId, skillId);
        var extraDescriptions = manager.getExtraDescriptions(categoryId, skillId);
        boolean mergeDescription = manager.shouldMergeDescriptions(categoryId, skillId);
        int maxLevel = manager.getMaxLevel(categoryId, skillId);
        var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(definitionId);
        String lootMode = config != null ? config.lootMode : null;

        int toggleLevel = manager.findMinimumToggleLevel(definitionId);

        // Use correct packet class name: SyncSkillDescriptionsPacket
        var packet = new SyncSkillDescriptionsPacket(definitionId, descriptions, extraDescriptions,
                mergeDescription, maxLevel, lootMode, new java.util.ArrayList<>(), toggleLevel);

        networkHandler.sendToPlayer(packet, player);
    }

    /**
     * COOLDOWN TRANSMISSION: Sends skill cooldown data
     */
    public static void sendToggleCooldown(ServerPlayerEntity player, Identifier categoryId, String skillId,
            int cooldownTicks) {
        var addon = SkillLevelingMod.getInstance();
        var handler = addon.getNetworkHandler();
        if (handler != null) {
            handler.sendToPlayer(new SyncToggleCooldownPacket(categoryId, skillId, cooldownTicks), player);
        }
    }

    /**
     * REQUEST TOGGLE TRANSMISSION: Sends toggle request to server
     */
    public static void sendRequestToggleSkill(Identifier categoryId, String skillId) {
        var addon = SkillLevelingMod.getInstance();
        var handler = addon.getNetworkHandler();
        if (handler != null) {
            handler.sendToServer(new RequestToggleSkillPacket(categoryId, skillId));
        }
    }

    /**
     * BULK SYNC: Sends all skill data to player at once
     * 
     * BULK MECHANICS: Efficient transmission of complete skill dataset
     * for initial connection or full resynchronization scenarios.
     */
    public static void sendFullSkillSync(ServerPlayerEntity player) {
        // COMPLETE SYNC: Send all available skill data
        syncPlayerSkillData(player);
    }

    // ================================================
    // CLIENT-SIDE PACKET HANDLERS (FUTURE IMPLEMENTATION)
    // ================================================

    /**
     * CLIENT PACKET PROCESSING: Handles incoming skill level updates
     * 
     * PROCESSING MECHANICS: Called when client receives skill level update
     * packets from server. Updates client-side cache and triggers UI refresh.
     */
    public static void handleSkillLevelUpdate(SkillLevelUpdatePacket packet) {
        // FUTURE IMPLEMENTATION: Process on client side
        // SkillLevelingClient.updateSkillLevel(
        // packet.getCategoryId(),
        // packet.getSkillId(),
        // packet.getCurrentLevel(),
        // packet.getMaxLevel()
        // );
    }

    /**
     * CLIENT DESCRIPTION PROCESSING: Handles incoming description updates
     * 
     * DESCRIPTION PROCESSING: Called when client receives skill description
     * update packets from server. Updates tooltip cache and UI elements.
     */
    public static void handleSkillDescriptionUpdate(SkillDescriptionUpdatePacket packet) {
        // FUTURE IMPLEMENTATION: Process on client side
        // SkillLevelingClient.updateSkillDescriptions(
        // packet.getCategoryId(),
        // packet.getSkillId(),
        // packet.getDescriptions(),
        // packet.getExtraDescriptions(),
        // packet.isMergeDescription()
        // );
    }
}
