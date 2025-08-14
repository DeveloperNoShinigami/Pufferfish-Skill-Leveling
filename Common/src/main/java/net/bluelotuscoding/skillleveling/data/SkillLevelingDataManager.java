package net.bluelotuscoding.skillleveling.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages persistent data for skill levels across server restarts
 */
public class SkillLevelingDataManager {
    
    // Map of player UUID -> category -> skill -> level
    private final Map<UUID, Map<Identifier, Map<String, Integer>>> playerSkillLevels;
    
    public SkillLevelingDataManager() {
        this.playerSkillLevels = new ConcurrentHashMap<>();
    }
    
    public void initialize(MinecraftServer server) {
        // Initialize data storage - could be file-based or integrated with core mod storage
        // For now, use in-memory storage
    }
    
    public void loadPlayerData(ServerPlayerEntity player) {
        // Load player skill level data from persistent storage
        // For now, initialize empty data if not exists
        playerSkillLevels.computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>());
    }
    
    public void savePlayerData(ServerPlayerEntity player) {
        // Save player skill level data to persistent storage
        // Implementation would write to files or database
    }
    
    /**
     * Get the level of a specific skill for a player
     */
    public int getSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return playerSkillLevels
                .computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(categoryId, k -> new ConcurrentHashMap<>())
                .getOrDefault(skillId, 1); // Default to level 1 when skill is unlocked
    }
    
    /**
     * Set the level of a specific skill for a player
     */
    public void setSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        playerSkillLevels
                .computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(categoryId, k -> new ConcurrentHashMap<>())
                .put(skillId, level);
    }
    
    /**
     * Get all skill levels for a player in a category
     */
    public Map<String, Integer> getCategorySkillLevels(ServerPlayerEntity player, Identifier categoryId) {
        return playerSkillLevels
                .computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(categoryId, k -> new ConcurrentHashMap<>());
    }
    
    /**
     * Reset all skill levels for a player in a category
     */
    public void resetCategorySkillLevels(ServerPlayerEntity player, Identifier categoryId) {
        var playerData = playerSkillLevels.get(player.getUuid());
        if (playerData != null) {
            playerData.remove(categoryId);
        }
    }
}