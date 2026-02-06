package net.bluelotuscoding.skillleveling.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.skillleveling.mixin_interface.SkillLevelHolder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ADDON COMPONENT: Manages persistent data for skill levels.
 * 
 * This manager stores all skill level data directly in the player's NBT,
 * ensuring
 * that data persists with the player across world transfers, server restarts,
 * and
 * even datapack namespace changes.
 * 
 * KEY DESIGN DECISIONS:
 * - Player NBT storage ensures data moves with the player
 * - Namespace-agnostic lookups handle datapack migrations gracefully
 * - Thread-safe concurrent data structures for multiplayer server support
 * - Graceful defaults (level 1) when Skills mod reports a skill as unlocked
 */
public class SkillLevelingDataManager {

    // In-memory cache of skill level data
    private final Map<UUID, Map<Identifier, Map<String, Integer>>> playerSkillLevels;

    // In-memory cache of activated reward levels (for persistence across restarts)
    // UUID -> CategoryId -> SkillId -> Set of Levels
    private final Map<UUID, Map<Identifier, Map<String, java.util.Set<Integer>>>> persistentActivatedLevels;

    public SkillLevelingDataManager() {
        this.playerSkillLevels = new ConcurrentHashMap<>();
        this.persistentActivatedLevels = new ConcurrentHashMap<>();
    }

    /**
     * ADDON INITIALIZATION: Called when the server starts.
     */
    public void initialize(MinecraftServer server) {
        // No file system initialization needed - data is stored in player NBT
    }

    /**
     * PLAYER JOIN: Loads skill data from player NBT into memory cache.
     */
    public void loadPlayerData(ServerPlayerEntity player) {
        playerSkillLevels.computeIfAbsent(player.getUuid(), uuid -> {
            var nbtData = loadFromNbt(player);
            if (nbtData != null && !nbtData.isEmpty()) {
                return nbtData;
            }
            return new ConcurrentHashMap<>();
        });
    }

    /**
     * PLAYER LEAVE: Saves skill data from memory cache to player NBT.
     */
    public void savePlayerData(ServerPlayerEntity player) {
        var data = playerSkillLevels.get(player.getUuid());
        var activated = persistentActivatedLevels.get(player.getUuid());
        saveToNbt(player, data, activated);
    }

    /**
     * SERVER SHUTDOWN: Clears the in-memory cache (data is already in NBT).
     */
    public void saveAll() {
        // Data is stored in player NBT, which is saved automatically by Minecraft
        playerSkillLevels.clear();
        persistentActivatedLevels.clear();
    }

    /**
     * CORE LEVEL RETRIEVAL: Gets the current level of a skill for a player.
     */
    public int getSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        var playerData = getPlayerData(player);
        var categoryData = getCategoryData(playerData, categoryId);
        return categoryData != null ? categoryData.getOrDefault(skillId, 0) : 0;
    }

    /**
     * LEVEL ADVANCEMENT: Sets a new level for a skill.
     */
    public void setSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        var playerData = getPlayerData(player);
        var categoryData = getCategoryData(playerData, categoryId);

        if (categoryData == null) {
            categoryData = new ConcurrentHashMap<>();
            playerData.put(categoryId, categoryData);
        }

        categoryData.put(skillId, level);

        // Immediate NBT update for persistence
        saveToNbt(player, playerData);
    }

    /**
     * Robust category data retrieval with namespace fallback.
     */
    private Map<String, Integer> getCategoryData(Map<Identifier, Map<String, Integer>> playerData,
            Identifier categoryId) {
        // Direct hit
        if (playerData.containsKey(categoryId)) {
            return playerData.get(categoryId);
        }

        // Namespace agnostic fallback: search by path
        for (var entry : playerData.entrySet()) {
            if (entry.getKey().getPath().equals(categoryId.getPath())) {
                // Found a match with different namespace!
                // Migrate the entry to the latest known namespace
                var data = entry.getValue();
                playerData.remove(entry.getKey());
                playerData.put(categoryId, data);

                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger().debug(
                        "[SkillLeveling] Migrated category data for " + categoryId.getPath() +
                                " from namespace " + entry.getKey().getNamespace() + " to "
                                + categoryId.getNamespace());

                return data;
            }
        }
        return null;
    }

    private Map<Identifier, Map<String, Integer>> getPlayerData(ServerPlayerEntity player) {
        return playerSkillLevels.computeIfAbsent(player.getUuid(), uuid -> {
            var nbtData = loadFromNbt(player);
            if (nbtData != null && !nbtData.isEmpty()) {
                return nbtData;
            }
            return new ConcurrentHashMap<>();
        });
    }

    /**
     * NBT PERSISTENCE: Saves data to player NBT.
     */
    public void saveToNbt(ServerPlayerEntity player, Map<Identifier, Map<String, Integer>> data) {
        saveToNbt(player, data, persistentActivatedLevels.get(player.getUuid()));
    }

    /**
     * NBT PERSISTENCE: Saves data to player NBT.
     */
    public void saveToNbt(ServerPlayerEntity player, Map<Identifier, Map<String, Integer>> data,
            Map<Identifier, Map<String, java.util.Set<Integer>>> activated) {
        net.minecraft.nbt.NbtCompound skillTag = new net.minecraft.nbt.NbtCompound();

        if (data != null && !data.isEmpty()) {
            net.minecraft.nbt.NbtCompound levelsTag = new net.minecraft.nbt.NbtCompound();
            for (var entry : data.entrySet()) {
                net.minecraft.nbt.NbtCompound catTag = new net.minecraft.nbt.NbtCompound();
                entry.getValue().forEach(catTag::putInt);
                levelsTag.put(entry.getKey().toString(), catTag);
            }
            skillTag.put("Levels", levelsTag);
        }

        if (activated != null && !activated.isEmpty()) {
            net.minecraft.nbt.NbtCompound activatedTag = new net.minecraft.nbt.NbtCompound();
            for (var entry : activated.entrySet()) {
                net.minecraft.nbt.NbtCompound catTag = new net.minecraft.nbt.NbtCompound();
                for (var skillEntry : entry.getValue().entrySet()) {
                    net.minecraft.nbt.NbtIntArray levelsArray = new net.minecraft.nbt.NbtIntArray(
                            skillEntry.getValue().stream().toList());
                    catTag.put(skillEntry.getKey(), levelsArray);
                }
                activatedTag.put(entry.getKey().toString(), catTag);
            }
            skillTag.put("ActivatedLevels", activatedTag);
        }

        if (skillTag.isEmpty())
            return;

        // Use our custom accessor interface
        if (player instanceof SkillLevelHolder holder) {
            holder.addon$setSkillLevelingData(skillTag);
        }
    }

    /**
     * NBT LOADING: Loads data from player NBT.
     */
    private Map<Identifier, Map<String, Integer>> loadFromNbt(ServerPlayerEntity player) {
        if (player instanceof SkillLevelHolder holder) {
            try {
                net.minecraft.nbt.NbtCompound skillTag = holder.addon$getSkillLevelingData();
                if (skillTag == null || skillTag.isEmpty()) {
                    return null;
                }
                Map<Identifier, Map<String, Integer>> data = new ConcurrentHashMap<>();
                Map<Identifier, Map<String, java.util.Set<Integer>>> activated = new ConcurrentHashMap<>();

                // Support legacy format (v1 had everything at root of SkillLevelingData)
                // New format: { Levels: {...}, ActivatedLevels: {...} }
                net.minecraft.nbt.NbtCompound levelsTag = skillTag.contains("Levels", 10)
                        ? skillTag.getCompound("Levels")
                        : skillTag;

                for (String key : levelsTag.getKeys()) {
                    if (key.equals("ActivatedLevels"))
                        continue; // Skip new tag if using old format

                    Identifier catId = Identifier.tryParse(key);
                    if (catId != null) {
                        net.minecraft.nbt.NbtCompound catTag = levelsTag.getCompound(key);
                        Map<String, Integer> skills = new ConcurrentHashMap<>();
                        for (String skillId : catTag.getKeys()) {
                            skills.put(skillId, catTag.getInt(skillId));
                        }
                        data.put(catId, skills);
                    }
                }

                if (skillTag.contains("ActivatedLevels", 10)) {
                    net.minecraft.nbt.NbtCompound activatedTag = skillTag.getCompound("ActivatedLevels");
                    for (String catKey : activatedTag.getKeys()) {
                        Identifier catId = Identifier.tryParse(catKey);
                        if (catId != null) {
                            net.minecraft.nbt.NbtCompound catTag = activatedTag.getCompound(catKey);
                            Map<String, java.util.Set<Integer>> skills = new ConcurrentHashMap<>();
                            for (String skillId : catTag.getKeys()) {
                                java.util.Set<Integer> levels = new java.util.concurrent.CopyOnWriteArraySet<>();
                                int[] array = catTag.getIntArray(skillId);
                                for (int i : array)
                                    levels.add(i);
                                skills.put(skillId, levels);
                            }
                            activated.put(catId, skills);
                        }
                    }
                }

                persistentActivatedLevels.put(player.getUuid(), activated);
                return data;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * BULK OPERATIONS: Category-level data access for admin commands and UI.
     */
    public Map<String, Integer> getCategorySkillLevels(ServerPlayerEntity player, Identifier categoryId) {
        var playerData = getPlayerData(player);
        return getCategoryData(playerData, categoryId);
    }

    /**
     * RESET FUNCTIONALITY: Clears all skill levels in a category.
     */
    public void resetCategorySkillLevels(ServerPlayerEntity player, Identifier categoryId) {
        var playerData = getPlayerData(player);
        // Find existing category to remove (case-insensitive for namespace)
        Identifier toRemove = null;
        for (Identifier id : playerData.keySet()) {
            if (id.getPath().equals(categoryId.getPath())) {
                toRemove = id;
                break;
            }
        }

        if (toRemove != null) {
            playerData.remove(toRemove);
            saveToNbt(player, playerData);

            // CLEANUP: If player has no skill data left, remove their entry entirely
            if (playerData.isEmpty()) {
                playerSkillLevels.remove(player.getUuid());
            }
        }
    }

    /**
     * DATA EXISTENCE CHECK: Determines if we have level data for a skill.
     */
    public boolean hasSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        var playerData = getPlayerData(player);
        var categoryData = getCategoryData(playerData, categoryId);
        return categoryData != null && categoryData.containsKey(skillId);
    }

    /**
     * INDIVIDUAL SKILL CLEANUP: Removes level data for a specific skill.
     */
    public void clearSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        var playerData = getPlayerData(player);
        var categoryData = getCategoryData(playerData, categoryId);
        if (categoryData != null) {
            categoryData.remove(skillId);
            // NESTED CLEANUP
            if (categoryData.isEmpty()) {
                playerData.remove(categoryId);
                if (playerData.isEmpty()) {
                    playerSkillLevels.remove(player.getUuid());
                }
            }
            saveToNbt(player, playerData);
        }
    }

    public java.util.Set<Integer> getActivatedLevels(UUID playerId, Identifier categoryId, String skillId) {
        var playerMap = persistentActivatedLevels.get(playerId);
        if (playerMap == null)
            return null;
        var catMap = playerMap.get(categoryId);
        if (catMap == null)
            return null;
        return catMap.get(skillId);
    }

    public void setActivatedLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        persistentActivatedLevels.computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(categoryId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(skillId, k -> new java.util.concurrent.CopyOnWriteArraySet<>())
                .add(level);

        savePlayerData(player);
    }
}