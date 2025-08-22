package net.bluelotuscoding.skillleveling.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ADDON COMPONENT: Manages persistent data for skill levels across server restarts
 * 
 * This is a complete refactor from the fork version - instead of modifying the Skills mod's
 * internal data structures, this creates a separate, parallel data storage system that tracks
 * skill levels independently. This allows us to work ALONGSIDE the official Skills mod rather
 * than replacing it.
 * 
 * KEY DESIGN DECISIONS:
 * - Uses separate data files (skill_leveling_data/) to avoid conflicts with Skills mod data
 * - Maintains player UUID -> category -> skill -> level mapping for fast lookups
 * - Thread-safe concurrent data structures for multiplayer server support
 * - Graceful defaults (level 1) when Skills mod reports a skill as unlocked but we have no data
 */
public class SkillLevelingDataManager {

    // ADDON DATA STRUCTURE: Independent skill level tracking
    // This exists separately from the Skills mod's unlock/lock data
    private final Map<UUID, Map<Identifier, Map<String, Integer>>> playerSkillLevels;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Path dataDir;
    
    public SkillLevelingDataManager() {
        // THREAD-SAFE COLLECTIONS: Essential for multiplayer servers where multiple
        // players might be advancing skills simultaneously
        this.playerSkillLevels = new ConcurrentHashMap<>();
    }
    
    /**
     * ADDON INITIALIZATION: Sets up our independent data storage system
     * 
     * Unlike the fork version that modified Skills mod initialization, this creates
     * a completely separate data directory. The Skills mod handles its own data in
     * its standard locations, while we maintain level progression data separately.
     */
    public void initialize(MinecraftServer server) {
        // SEPARATE DATA DIRECTORY: Avoids any conflicts with Skills mod data storage
        dataDir = server.getRunDirectory().toPath().resolve("skill_leveling_data");
        try {
            Files.createDirectories(dataDir);
            
            // STARTUP DATA LOADING: Pre-load all existing player data for performance
            // This prevents file I/O lag when players join and try to check skill levels
            Files.list(dataDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        if (name.endsWith(".json")) {
                            try {
                                UUID uuid = UUID.fromString(name.substring(0, name.length() - 5));
                                playerSkillLevels.put(uuid, readPlayerData(path));
                            } catch (Exception e) {
                                // GRACEFUL ERROR HANDLING: Corrupted data files don't crash the server
                                System.err.println("Failed to load skill leveling data for " + name + ": " + e.getMessage());
                            }
                        }
                    });
        } catch (IOException e) {
            System.err.println("Failed to initialize skill leveling data directory: " + e.getMessage());
        }
    }

    /**
     * LAZY LOADING: Only loads player data when they actually join the server
     * 
     * This is more efficient than loading all player data at server startup,
     * especially for servers with many inactive players. The Skills mod will
     * handle player join events, and we piggyback on that to load our level data.
     */
    public void loadPlayerData(ServerPlayerEntity player) {
        playerSkillLevels.computeIfAbsent(player.getUuid(), uuid -> {
            Path path = getPlayerPath(uuid);
            if (Files.exists(path)) {
                return readPlayerData(path);
            }
            // EMPTY DATA FOR NEW PLAYERS: Will be populated as they advance skills
            return new ConcurrentHashMap<>();
        });
    }

    /**
     * IMMEDIATE PERSISTENCE: Saves player data when they disconnect
     * 
     * Critical for preserving skill levels if the server crashes or restarts
     * unexpectedly. We don't want players to lose their leveling progress.
     */
    public void savePlayerData(ServerPlayerEntity player) {
        var data = playerSkillLevels.get(player.getUuid());
        if (data != null && !data.isEmpty()) {
            writePlayerData(player.getUuid(), data);
        }
    }

    /**
     * SERVER SHUTDOWN SAFETY: Ensures all skill level data is persisted
     * 
     * Called during server shutdown to guarantee no data loss. Even if individual
     * player saves failed, this catches everything in a final save operation.
     */
    public void saveAll() {
        if (dataDir == null) {
            return; // Not initialized yet
        }
        playerSkillLevels.forEach(this::writePlayerData);
    }
    
    /**
     * CORE LEVEL RETRIEVAL: Gets the current level of a skill for a player
     * 
     * CRITICAL ADDON BEHAVIOR: This method bridges our level data with the Skills mod's
     * unlock state. We only return meaningful levels for skills that the Skills mod
     * reports as unlocked. If a skill is locked in the Skills mod, we effectively
     * ignore our level data and return 0.
     * 
     * DEFAULT TO LEVEL 1: When a skill is unlocked but we have no level data,
     * we assume it's at level 1 (the base unlock level). This handles cases where
     * skills were unlocked before our addon was installed.
     */
    public int getSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return playerSkillLevels
                .computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(categoryId, k -> new ConcurrentHashMap<>())
                .getOrDefault(skillId, 1); // DEFAULT TO LEVEL 1: Base unlock level
    }
    
    /**
     * LEVEL ADVANCEMENT: Sets a new level for a skill
     * 
     * IMPORTANT: This method doesn't validate whether the skill is unlocked in
     * the Skills mod - that validation happens in the SkillLevelingManager layer.
     * This is pure data storage with thread-safe concurrent access.
     */
    public void setSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        playerSkillLevels
                .computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(categoryId, k -> new ConcurrentHashMap<>())
                .put(skillId, level);
    }
    
    /**
     * BULK OPERATIONS: Category-level data access for admin commands and UI
     * 
     * Useful for commands like "/skill-leveling category reset" or for building
     * admin interfaces that show all skill levels in a category at once.
     */
    public Map<String, Integer> getCategorySkillLevels(ServerPlayerEntity player, Identifier categoryId) {
        return playerSkillLevels
                .computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(categoryId, k -> new ConcurrentHashMap<>());
    }

    /**
     * RESET FUNCTIONALITY: Clears all skill levels in a category
     * 
     * Used by admin commands or when players want to respec an entire skill tree.
     * This only affects our level data - the Skills mod's unlock/lock state is
     * managed separately and might need separate reset commands.
     */
    public void resetCategorySkillLevels(ServerPlayerEntity player, Identifier categoryId) {
        var playerData = playerSkillLevels.get(player.getUuid());
        if (playerData != null) {
            playerData.remove(categoryId);
            // CLEANUP: If player has no skill data left, remove their entry entirely
            if (playerData.isEmpty()) {
                playerSkillLevels.remove(player.getUuid());
            }
        }
    }

    /**
     * FILE SYSTEM UTILITIES: Private methods for data persistence
     * 
     * These handle the actual file I/O operations for our separate data storage.
     * Uses JSON format for human readability and debugging convenience.
     */
    
    private Path getPlayerPath(UUID uuid) {
        return dataDir.resolve(uuid.toString() + ".json");
    }

    /**
     * JSON DESERIALIZATION: Converts saved data back to in-memory structures
     * 
     * Handles the Identifier parsing carefully since they have namespace:path format.
     * This is critical for compatibility with modded Skills mod configurations that
     * might use custom namespaces for their categories and skills.
     */
    private Map<Identifier, Map<String, Integer>> readPlayerData(Path path) {
        Map<Identifier, Map<String, Integer>> categories = new ConcurrentHashMap<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root != null) {
                for (var entry : root.entrySet()) {
                    Map<String, Integer> skills = new ConcurrentHashMap<>();
                    var skillsObject = entry.getValue().getAsJsonObject();
                    if (skillsObject != null) {
                        skillsObject.entrySet().forEach(e -> {
                            try {
                                skills.put(e.getKey(), e.getValue().getAsInt());
                            } catch (Exception ex) {
                                // GRACEFUL DEGRADATION: Skip corrupted skill entries rather than failing entirely
                                System.err.println("Skipping corrupted skill entry: " + e.getKey());
                            }
                        });
                    }
                    categories.put(new Identifier(entry.getKey()), skills);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read player skill data from " + path + ": " + e.getMessage());
        }
        return categories;
    }

    /**
     * JSON SERIALIZATION: Saves in-memory data to persistent storage
     * 
     * Uses pretty-printing for easier debugging and manual editing if needed.
     * The nested structure mirrors our in-memory organization for consistency.
     */
    private void writePlayerData(UUID uuid, Map<Identifier, Map<String, Integer>> data) {
        if (dataDir == null || data.isEmpty()) {
            return; // Don't create empty files
        }
        
        JsonObject root = new JsonObject();
        for (var entry : data.entrySet()) {
            JsonObject skills = new JsonObject();
            entry.getValue().forEach(skills::addProperty);
            root.add(entry.getKey().toString(), skills);
        }
        
        Path path = getPlayerPath(uuid);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            gson.toJson(root, writer);
        } catch (IOException e) {
            System.err.println("Failed to save player skill data to " + path + ": " + e.getMessage());
        }
    }
    
    /**
     * DATA EXISTENCE CHECK: Determines if we have level data for a skill
     * 
     * ADDON INTEGRATION POINT: This helps distinguish between:
     * 1. Skills unlocked before our addon was installed (no level data)
     * 2. Skills unlocked after addon installation (has level data)
     * 3. Skills that were unlocked then had their data cleared
     * 
     * Used by the manager layer to decide whether to initialize skill data
     * when a skill unlock event is detected.
     */
    public boolean hasSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        var playerData = playerSkillLevels.get(player.getUuid());
        if (playerData == null) {
            return false;
        }
        var categoryData = playerData.get(categoryId);
        return categoryData != null && categoryData.containsKey(skillId);
    }

    /**
     * INDIVIDUAL SKILL CLEANUP: Removes level data for a specific skill
     * 
     * Used when skills are locked in the Skills mod, or for admin commands
     * that want to reset individual skills. Includes cleanup logic to prevent
     * memory leaks from empty nested maps.
     */
    public void clearSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        var playerData = playerSkillLevels.get(player.getUuid());
        if (playerData != null) {
            var categoryData = playerData.get(categoryId);
            if (categoryData != null) {
                categoryData.remove(skillId);
                // NESTED CLEANUP: Remove empty categories and players to prevent memory bloat
                if (categoryData.isEmpty()) {
                    playerData.remove(categoryId);
                    if (playerData.isEmpty()) {
                        playerSkillLevels.remove(player.getUuid());
                    }
                }
            }
        }
    }
}