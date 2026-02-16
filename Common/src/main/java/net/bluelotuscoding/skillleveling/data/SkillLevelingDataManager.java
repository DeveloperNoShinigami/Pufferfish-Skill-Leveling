package net.bluelotuscoding.skillleveling.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.skillleveling.mixin_interface.SkillLevelHolder;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ADDON COMPONENT: Manages persistent data for skill levels.
 * 
 * This manager stores all skill level data directly in the player's NBT,
 * ensuring that data persists with the player across world transfers,
 * server restarts, and even datapack namespace changes.
 * 
 * KEY DESIGN DECISIONS:
 * - Player NBT storage ensures data moves with the player
 * - Namespace-agnostic lookups handle datapack migrations gracefully
 * - Thread-safe concurrent data structures for multiplayer server support
 */
public class SkillLevelingDataManager {

    // Unified in-memory cache of player data (Levels + Toggles)
    private final Map<UUID, PlayerCache> playerCaches;

    private static class PlayerCache {
        final Map<Identifier, Map<String, Integer>> levels = new ConcurrentHashMap<>();
        final Map<Identifier, Map<String, Boolean>> toggles = new ConcurrentHashMap<>();
        final Set<Identifier> unlockedCategories = ConcurrentHashMap.newKeySet();
    }

    public SkillLevelingDataManager() {
        this.playerCaches = new ConcurrentHashMap<>();
    }

    public void initialize(MinecraftServer server) {
        // No file system initialization needed - data is stored in player NBT
    }

    /**
     * Atomic retrieval of player cache. Loads from NBT if missing.
     */
    private PlayerCache getPlayerCache(ServerPlayerEntity player) {
        return playerCaches.computeIfAbsent(player.getUuid(), uuid -> {
            PlayerCache cache = new PlayerCache();
            loadFromNbt(player, cache);
            return cache;
        });
    }

    public void loadPlayerData(ServerPlayerEntity player) {
        getPlayerCache(player); // Triggers loading
    }

    public void savePlayerData(ServerPlayerEntity player) {
        var cache = playerCaches.get(player.getUuid());
        if (cache != null) {
            saveToNbt(player, cache);
        }
    }

    public void saveAll() {
        playerCaches.clear();
    }

    public int getSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        var cache = getPlayerCache(player);
        var categoryData = getCategoryData(cache.levels, categoryId);
        return categoryData != null ? categoryData.getOrDefault(skillId, 0) : 0;
    }

    public void setSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        var cache = getPlayerCache(player);
        var categoryData = getCategoryData(cache.levels, categoryId);

        if (categoryData == null) {
            categoryData = new ConcurrentHashMap<>();
            cache.levels.put(categoryId, categoryData);
        }

        categoryData.put(skillId, level);
        saveToNbt(player, cache);
    }

    public void saveToNbt(ServerPlayerEntity player, PlayerCache cache) {
        net.minecraft.nbt.NbtCompound skillTag = new net.minecraft.nbt.NbtCompound();

        // Save Levels
        if (!cache.levels.isEmpty()) {
            net.minecraft.nbt.NbtCompound levelsTag = new net.minecraft.nbt.NbtCompound();
            for (var entry : cache.levels.entrySet()) {
                net.minecraft.nbt.NbtCompound catTag = new net.minecraft.nbt.NbtCompound();
                entry.getValue().forEach(catTag::putInt);
                levelsTag.put(entry.getKey().toString(), catTag);
            }
            skillTag.put("Levels", levelsTag);
        }

        // Save Toggle States
        if (!cache.toggles.isEmpty()) {
            net.minecraft.nbt.NbtCompound togglesTag = new net.minecraft.nbt.NbtCompound();
            for (var entry : cache.toggles.entrySet()) {
                net.minecraft.nbt.NbtCompound catTag = new net.minecraft.nbt.NbtCompound();
                entry.getValue().forEach(catTag::putBoolean);
                togglesTag.put(entry.getKey().toString(), catTag);
            }
            skillTag.put("ToggleStates", togglesTag);
        }

        // Save Unlocked Categories (for keep_unlocked persistence)
        if (!cache.unlockedCategories.isEmpty()) {
            net.minecraft.nbt.NbtList unlockedList = new net.minecraft.nbt.NbtList();
            for (Identifier catId : cache.unlockedCategories) {
                unlockedList.add(net.minecraft.nbt.NbtString.of(catId.toString()));
            }
            skillTag.put("UnlockedCategories", unlockedList);
        }

        if (player instanceof SkillLevelHolder holder) {
            holder.addon$setSkillLevelingData(skillTag);
        }
    }

    private void loadFromNbt(ServerPlayerEntity player, PlayerCache cache) {
        if (player instanceof SkillLevelHolder holder) {
            try {
                net.minecraft.nbt.NbtCompound skillTag = holder.addon$getSkillLevelingData();
                if (skillTag == null || skillTag.isEmpty())
                    return;

                // Load Levels
                net.minecraft.nbt.NbtCompound levelsTag = skillTag.contains("Levels", 10)
                        ? skillTag.getCompound("Levels")
                        : skillTag;

                for (String key : levelsTag.getKeys()) {
                    Identifier catId = Identifier.tryParse(key);
                    if (catId != null) {
                        net.minecraft.nbt.NbtCompound catTag = levelsTag.getCompound(key);
                        Map<String, Integer> skills = new ConcurrentHashMap<>();
                        for (String skillId : catTag.getKeys()) {
                            if (catTag.contains(skillId, 3) || catTag.contains(skillId, 1)
                                    || catTag.contains(skillId, 2)) {
                                skills.put(skillId, catTag.getInt(skillId));
                            }
                        }
                        if (!skills.isEmpty())
                            cache.levels.put(catId, skills);
                    }
                }

                // Load Toggle States
                if (skillTag.contains("ToggleStates", 10)) {
                    net.minecraft.nbt.NbtCompound togglesTag = skillTag.getCompound("ToggleStates");
                    for (String key : togglesTag.getKeys()) {
                        Identifier catId = Identifier.tryParse(key);
                        if (catId != null) {
                            net.minecraft.nbt.NbtCompound catTag = togglesTag.getCompound(key);
                            Map<String, Boolean> toggles = new ConcurrentHashMap<>();
                            for (String skillId : catTag.getKeys()) {
                                toggles.put(skillId, catTag.getBoolean(skillId));
                            }
                            cache.toggles.put(catId, toggles);
                        }
                    }
                }

                // Load Unlocked Categories (for keep_unlocked persistence)
                if (skillTag.contains("UnlockedCategories", 9)) {
                    net.minecraft.nbt.NbtList unlockedList = skillTag.getList("UnlockedCategories", 8);
                    for (int i = 0; i < unlockedList.size(); i++) {
                        Identifier catId = Identifier.tryParse(unlockedList.getString(i));
                        if (catId != null) {
                            cache.unlockedCategories.add(catId);
                        }
                    }
                }
            } catch (Exception e) {
                // Silently fail and use empty maps
            }
        }
    }

    public Map<String, Integer> getCategorySkillLevels(ServerPlayerEntity player, Identifier categoryId) {
        var cache = getPlayerCache(player);
        return getCategoryData(cache.levels, categoryId);
    }

    public void resetCategorySkillLevels(ServerPlayerEntity player, Identifier categoryId) {
        var cache = getPlayerCache(player);

        Identifier toRemoveLevel = null;
        for (Identifier id : cache.levels.keySet()) {
            if (id.getPath().equals(categoryId.getPath())) {
                toRemoveLevel = id;
                break;
            }
        }

        Identifier toRemoveToggle = null;
        for (Identifier id : cache.toggles.keySet()) {
            if (id.getPath().equals(categoryId.getPath())) {
                toRemoveToggle = id;
                break;
            }
        }

        if (toRemoveLevel != null)
            cache.levels.remove(toRemoveLevel);
        if (toRemoveToggle != null)
            cache.toggles.remove(toRemoveToggle);

        saveToNbt(player, cache);
    }

    public boolean hasSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        var cache = getPlayerCache(player);
        var categoryData = getCategoryData(cache.levels, categoryId);
        return categoryData != null && categoryData.containsKey(skillId);
    }

    public void clearSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        var cache = getPlayerCache(player);
        var catLevelData = getCategoryData(cache.levels, categoryId);
        if (catLevelData != null) {
            catLevelData.remove(skillId);
            if (catLevelData.isEmpty()) {
                // Find and remove by exact key
                cache.levels.keySet().removeIf(id -> id.getPath().equals(categoryId.getPath()));
            }
        }

        var catToggleData = getCategoryData(cache.toggles, categoryId);
        if (catToggleData != null) {
            catToggleData.remove(skillId);
            if (catToggleData.isEmpty()) {
                cache.toggles.keySet().removeIf(id -> id.getPath().equals(categoryId.getPath()));
            }
        }

        saveToNbt(player, cache);
    }

    public boolean isToggleActive(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        var cache = getPlayerCache(player);
        var catToggleData = getCategoryData(cache.toggles, categoryId);
        return catToggleData != null && catToggleData.getOrDefault(skillId, false);
    }

    public void setToggleActive(ServerPlayerEntity player, Identifier categoryId, String skillId, boolean active) {
        var cache = getPlayerCache(player);
        var catToggleData = getCategoryData(cache.toggles, categoryId);

        if (catToggleData == null) {
            catToggleData = new ConcurrentHashMap<>();
            cache.toggles.put(categoryId, catToggleData);
        }

        catToggleData.put(skillId, active);
        saveToNbt(player, cache);
    }

    /**
     * Check if a category has been previously unlocked for this player.
     * Used with keep_unlocked to persist unlock state across sessions.
     */
    public boolean isCategoryPreviouslyUnlocked(ServerPlayerEntity player, Identifier categoryId) {
        var cache = getPlayerCache(player);
        return cache.unlockedCategories.contains(categoryId);
    }

    /**
     * Mark a category as having been unlocked for this player.
     * Persisted to NBT so it survives across relogs.
     */
    public void markCategoryUnlocked(ServerPlayerEntity player, Identifier categoryId) {
        var cache = getPlayerCache(player);
        if (cache.unlockedCategories.add(categoryId)) {
            saveToNbt(player, cache);
        }
    }

    /**
     * Remove a category from the previously-unlocked set (e.g. on skill reset).
     */
    public void markCategoryLocked(ServerPlayerEntity player, Identifier categoryId) {
        var cache = getPlayerCache(player);
        if (cache.unlockedCategories.remove(categoryId)) {
            saveToNbt(player, cache);
        }
    }

    private <T> Map<String, T> getCategoryData(Map<Identifier, Map<String, T>> playerData,
            Identifier categoryId) {
        if (playerData.containsKey(categoryId)) {
            return playerData.get(categoryId);
        }

        for (var entry : playerData.entrySet()) {
            if (entry.getKey().getPath().equals(categoryId.getPath())) {
                var data = entry.getValue();
                if (!entry.getKey().equals(categoryId)) {
                    playerData.remove(entry.getKey());
                    playerData.put(categoryId, data);
                }
                return data;
            }
        }
        return null;
    }
}