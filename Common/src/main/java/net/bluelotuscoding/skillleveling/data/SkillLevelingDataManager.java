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
 * Manages persistent data for skill levels across server restarts
 */
public class SkillLevelingDataManager {

    // Map of player UUID -> category -> skill -> level
    private final Map<UUID, Map<Identifier, Map<String, Integer>>> playerSkillLevels;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Path dataDir;
    
    public SkillLevelingDataManager() {
        this.playerSkillLevels = new ConcurrentHashMap<>();
    }
    
    public void initialize(MinecraftServer server) {
        dataDir = server.getRunDirectory().toPath().resolve("skill_leveling_data");
        try {
            Files.createDirectories(dataDir);
            Files.list(dataDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        if (name.endsWith(".json")) {
                            try {
                                UUID uuid = UUID.fromString(name.substring(0, name.length() - 5));
                                playerSkillLevels.put(uuid, readPlayerData(path));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadPlayerData(ServerPlayerEntity player) {
        playerSkillLevels.computeIfAbsent(player.getUuid(), uuid -> {
            Path path = getPlayerPath(uuid);
            if (Files.exists(path)) {
                return readPlayerData(path);
            }
            return new ConcurrentHashMap<>();
        });
    }

    public void savePlayerData(ServerPlayerEntity player) {
        var data = playerSkillLevels.get(player.getUuid());
        if (data != null) {
            writePlayerData(player.getUuid(), data);
        }
    }

    public void saveAll() {
        if (dataDir == null) {
            return;
        }
        playerSkillLevels.forEach(this::writePlayerData);
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

    private Path getPlayerPath(UUID uuid) {
        return dataDir.resolve(uuid.toString() + ".json");
    }

    private Map<Identifier, Map<String, Integer>> readPlayerData(Path path) {
        Map<Identifier, Map<String, Integer>> categories = new ConcurrentHashMap<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            for (var entry : root.entrySet()) {
                Map<String, Integer> skills = new ConcurrentHashMap<>();
                entry.getValue().getAsJsonObject().entrySet()
                        .forEach(e -> skills.put(e.getKey(), e.getValue().getAsInt()));
                categories.put(new Identifier(entry.getKey()), skills);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return categories;
    }

    private void writePlayerData(UUID uuid, Map<Identifier, Map<String, Integer>> data) {
        if (dataDir == null) {
            return;
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
            e.printStackTrace();
        }
    }
    
    /**
     * Check if a specific skill level entry exists for a player.
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
     * Remove a specific skill level entry for a player.
     */
    public void clearSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        var playerData = playerSkillLevels.get(player.getUuid());
        if (playerData != null) {
            var categoryData = playerData.get(categoryId);
            if (categoryData != null) {
                categoryData.remove(skillId);
                if (categoryData.isEmpty()) {
                    playerData.remove(categoryId);
                }
            }
        }
    }
}