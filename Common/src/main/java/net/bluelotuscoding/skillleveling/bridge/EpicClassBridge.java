package net.bluelotuscoding.skillleveling.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef;
import net.minecraft.resource.ResourceManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;

public final class EpicClassBridge {
    private static final Map<String, java.util.List<Identifier>> CLASS_TO_CATEGORY = new HashMap<>();
    private static final Map<Identifier, String> CATEGORY_TO_CLASS = new HashMap<>();
    private static final Map<String, java.util.List<String>> RAW_CLASS_MAPPINGS = new HashMap<>();
    private static final Map<String, Identifier> PASSIVE_TO_SKILL = new HashMap<>();
    private static final Map<String, Integer> PASSIVE_TO_LEVEL = new HashMap<>();
    private static final Map<UUID, Identifier> PLAYER_ACTIVE_CATEGORY = new HashMap<>();

    /**
     * Cache of Pufferfish skill display strings, loaded from the server's
     * ResourceManager.
     * Key: "categoryId:skillId" (e.g. "necromancer:skeleton_mastery")
     * Value: [title, description] as plain strings from definitions.json
     */
    private static final Map<String, String[]> SKILL_DISPLAY_CACHE = new HashMap<>();
    private static final Gson SKILL_GSON = new Gson();

    private static final Map<String, Identifier> RESOLVED_MAPPINGS = new HashMap<>();

    private static boolean enabled = false;
    private static boolean autoActivateCategory = true;
    private static boolean lockOtherCategories = true;
    private static boolean syncOnLogin = true;

    private EpicClassBridge() {
    }

    public static void loadConfig(BridgeConfig config) {
        if (config == null) {
            config = new BridgeConfig();
        }

        enabled = config.enabled;
        autoActivateCategory = config.autoActivateCategory;
        lockOtherCategories = config.lockOtherCategories;
        syncOnLogin = config.syncOnLogin;

        RAW_CLASS_MAPPINGS.clear();
        CLASS_TO_CATEGORY.clear();
        CATEGORY_TO_CLASS.clear();
        RESOLVED_MAPPINGS.clear();

        // Store raw mappings for later resolution
        for (var entry : config.classToCategoryMap.entrySet()) {
            String className = normalizeClassName(entry.getKey());
            if (!className.isEmpty() && entry.getValue() != null && !entry.getValue().isEmpty()) {
                RAW_CLASS_MAPPINGS.put(className, entry.getValue());
            }
        }

        PASSIVE_TO_SKILL.clear();
        PASSIVE_TO_LEVEL.clear();
        for (var entry : config.classPassiveToSkillMap.entrySet()) {
            String passiveKey = entry.getKey().trim().toUpperCase(Locale.ROOT);
            Identifier skillId = resolveSkillId(entry.getValue());
            if (skillId != null) {
                PASSIVE_TO_SKILL.put(passiveKey, skillId);

                // Also load the level requirement if it exists
                Integer level = config.classPassiveToLevelMap.get(entry.getKey());
                if (level != null) {
                    PASSIVE_TO_LEVEL.put(passiveKey, level);
                }
            }
        }

        logInfo("Epic Class bridge config loaded (" + RAW_CLASS_MAPPINGS.size() + " mappings)");
    }

    /**
     * Resolves category IDs from raw string mappings.
     * Called lazily when first needed after Pufferfish Skills is initialized.
     */
    private static void ensureCategoriesResolved() {
        if (RAW_CLASS_MAPPINGS.isEmpty() || RESOLVED_MAPPINGS.size() == RAW_CLASS_MAPPINGS.size()) {
            return;
        }

        // Check if Skills API is ready - it must have at least one category to be
        // considered ready
        try {
            if (SkillsAPI.streamCategories().findAny().isEmpty()) {
                return; // Not ready yet
            }
        } catch (Exception e) {
            return; // Not ready yet
        }

        for (var entry : RAW_CLASS_MAPPINGS.entrySet()) {
            String className = entry.getKey().toLowerCase(java.util.Locale.ROOT);
            if (RESOLVED_MAPPINGS.containsKey(className)) {
                continue;
            }

            java.util.List<Identifier> categoryIds = new java.util.ArrayList<>();
            boolean allResolved = true;
            for (String rawCat : entry.getValue()) {
                Identifier categoryId = resolveCategoryId(rawCat);
                if (categoryId != null) {
                    categoryIds.add(categoryId);
                    CATEGORY_TO_CLASS.put(categoryId, className);
                } else {
                    allResolved = false;
                    logWarn("[Bridge] Failed to resolve category for '" + className + "' (raw lookup: " + rawCat + ")");
                }
            }

            if (!categoryIds.isEmpty()) {
                CLASS_TO_CATEGORY.put(className, categoryIds);
                Identifier activeCategory = resolveCategoryId(className);
                if (activeCategory == null) {
                    activeCategory = new Identifier("puffish_skills", className.toLowerCase(java.util.Locale.ROOT));
                }
                RESOLVED_MAPPINGS.put(className, activeCategory);
                logInfo("[Bridge] Resolved mapping: '" + className + "' -> " + categoryIds.size()
                        + " categories (Active branch: " + activeCategory + ")");
            }
        }

        if (RESOLVED_MAPPINGS.size() == RAW_CLASS_MAPPINGS.size()) {
            logInfo("Epic Class bridge mappings FULLY resolved: " + CLASS_TO_CATEGORY.size() + " categories");
        }
    }

    public static void clearResolutionCache() {
        CLASS_TO_CATEGORY.clear();
        CATEGORY_TO_CLASS.clear();
        RESOLVED_MAPPINGS.clear();
        PLAYER_ACTIVE_CATEGORY.clear();
        logInfo("[Bridge] Resolution caches cleared.");
    }

    /**
     * Reads the Pufferfish skill definitions.json for every registered
     * skill_category_id
     * using the server's ResourceManager, and caches title + description for each
     * skill.
     * Call this on server start / world load, before the class select screen is
     * opened.
     *
     * @param rm the server resource manager (from
     *           MinecraftServer.getResourceManager())
     */
    public static void loadSkillDisplayData(ResourceManager rm) {
        SKILL_DISPLAY_CACHE.clear();
        var defs = EpicClassConfigManager.getClasses();
        if (defs == null || defs.isEmpty()) {
            return;
        }

        for (var def : defs.values()) {
            if (def.skill_category_id == null || def.gui_passives == null) {
                continue;
            }
            String catId = def.skill_category_id;

            // Try every namespace in the resource manager for the definitions.json
            for (String ns : rm.getAllNamespaces()) {
                Identifier resId = new Identifier(ns,
                        "puffish_skills/categories/" + catId + "/definitions.json");
                var optResource = rm.getResource(resId);
                if (optResource.isEmpty()) {
                    continue;
                }
                try (var stream = optResource.get().getInputStream();
                        var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    JsonObject root = SKILL_GSON.fromJson(reader, JsonObject.class);
                    if (root == null) {
                        continue;
                    }
                    for (var skillEntry : root.entrySet()) {
                        String skillId = skillEntry.getKey();
                        JsonObject skillObj = skillEntry.getValue().getAsJsonObject();
                        String title = skillObj.has("title")
                                ? skillObj.get("title").getAsString()
                                : skillId.replace("_", " ");
                        String description = "";
                        if (skillObj.has("description")) {
                            var descElem = skillObj.get("description");
                            if (descElem.isJsonArray()) {
                                StringBuilder sb = new StringBuilder();
                                var arr = descElem.getAsJsonArray();
                                for (int i = 0; i < arr.size(); i++) {
                                    if (i > 0)
                                        sb.append("\n");
                                    sb.append(arr.get(i).getAsString());
                                }
                                description = sb.toString();
                            } else {
                                description = descElem.getAsString();
                            }
                        }
                        SKILL_DISPLAY_CACHE.put(catId + ":" + skillId, new String[] { title, description });
                    }
                    logInfo("Loaded skill display data for category '" + catId
                            + "' (" + root.size() + " skills) from namespace '" + ns + "'");
                    break; // Found it in this namespace, stop scanning
                } catch (Exception e) {
                    logWarn("Failed to load skill display data for " + resId + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Returns [title, description] for a Pufferfish skill from the cached
     * definitions.json data.
     * Returns null if the skill entry is not found in the cache.
     *
     * @param categoryId the skill_category_id (e.g. "necromancer")
     * @param skillId    the pufferfish_skill_id (e.g. "skeleton_mastery")
     */
    public static String[] getSkillDisplay(String categoryId, String skillId) {
        return SKILL_DISPLAY_CACHE.get(categoryId + ":" + skillId);
    }

    /** Clears the skill display cache (call on world unload / server stop). */
    public static void invalidateSkillDisplayCache() {
        SKILL_DISPLAY_CACHE.clear();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean shouldSyncOnLogin() {
        return syncOnLogin;
    }

    /**
     * Returns all configured category IDs from the bridge config.
     * Useful for fallback scenarios when player context isn't available.
     */
    public static java.util.Set<Identifier> getConfiguredCategories() {
        ensureCategoriesResolved();
        java.util.Set<Identifier> allCategories = new java.util.HashSet<>();
        for (java.util.List<Identifier> list : CLASS_TO_CATEGORY.values()) {
            allCategories.addAll(list);
        }
        return allCategories;
    }

    public static Optional<String> getClassForCategory(Identifier categoryId) {
        ensureCategoriesResolved();
        return Optional.ofNullable(CATEGORY_TO_CLASS.get(categoryId));
    }

    public static java.util.List<Identifier> getCategoriesForClass(String classTypeName) {
        ensureCategoriesResolved();
        String normalized = normalizeClassName(classTypeName);
        java.util.List<Identifier> categoryIds = CLASS_TO_CATEGORY.get(normalized);
        if (categoryIds == null || categoryIds.isEmpty()) {
            // Fallback for custom classes: check the EpicClassDef directly
            // getClassDef now handles normalization and prefixes internally.
            EpicClassDef def = EpicClassConfigManager.getClassDef(classTypeName);
            if (def != null && def.skill_category_id != null) {
                Identifier categoryId = resolveCategoryId(def.skill_category_id);
                if (categoryId != null) {
                    return java.util.Collections.singletonList(categoryId);
                }
            }
            return java.util.Collections.emptyList();
        }
        return categoryIds;
    }

    public static Optional<Identifier> getCategoryForClass(String classTypeName) {
        ensureCategoriesResolved();
        String normalized = normalizeClassName(classTypeName);
        Identifier resolved = RESOLVED_MAPPINGS.get(normalized);
        if (resolved != null) {
            return Optional.of(resolved);
        }
        return getCategoriesForClass(classTypeName).stream().findFirst();
    }

    public static boolean isPassiveMapped(String className, int slot) {
        if (!enabled)
            return false;
        String key = normalizeClassName(className) + "_" + slot;
        return PASSIVE_TO_SKILL.containsKey(key);
    }

    public static String getMappedSkillId(String className, int slot) {
        String key = normalizeClassName(className) + "_" + slot;
        Identifier id = PASSIVE_TO_SKILL.get(key);
        return id != null ? id.toString() : null;
    }

    public static int getRequiredLevel(String className, int slot) {
        String key = normalizeClassName(className) + "_" + slot;
        return PASSIVE_TO_LEVEL.getOrDefault(key, 0);
    }

    public static boolean isPassiveUnlocked(Object player, String className, int slot) {
        if (!enabled)
            return false;

        String key = normalizeClassName(className) + "_" + slot;
        Identifier skillId = PASSIVE_TO_SKILL.get(key);

        if (skillId == null) {
            return false;
        }

        if (!(player instanceof PlayerEntity sp)) {
            return false;
        }

        return getCategoryForClass(className)
                .flatMap(catId -> SkillsAPI.getCategory(catId))
                .flatMap(category -> category.getSkill(skillId.getPath()))
                .map((Skill skill) -> {
                    if (sp instanceof ServerPlayerEntity ssp) {
                        Skill.State state = skill.getState(ssp);
                        return state == Skill.State.UNLOCKED;
                    }
                    return false;
                })
                .orElse(false);
    }

    public static boolean isPassiveVisible(Object player, String className, int slot) {
        if (!enabled)
            return true;

        String key = normalizeClassName(className) + "_" + slot;
        Identifier skillId = PASSIVE_TO_SKILL.get(key);

        if (skillId == null) {
            return true;
        }

        if (!(player instanceof PlayerEntity sp)) {
            return true;
        }

        return getCategoryForClass(className)
                .flatMap(catId -> SkillsAPI.getCategory(catId))
                .flatMap(category -> category.getSkill(skillId.getPath()))
                .map((Skill skill) -> {
                    if (sp instanceof ServerPlayerEntity ssp) {
                        Skill.State state = skill.getState(ssp);
                        if (state == Skill.State.UNLOCKED)
                            return true;
                    }

                    return !isSkillHidden(skill);
                })
                .orElse(true);
    }

    private static boolean isSkillHidden(net.puffish.skillsmod.api.Skill skill) {
        try {
            // Check for isHidden method via reflection
            var method = skill.getClass().getMethod("isHidden");
            return (boolean) method.invoke(skill);
        } catch (Exception e) {
            return false; // Default to visible if cannot determine
        }
    }

    public static Optional<Identifier> getActiveCategory(Object player) {
        if (player == null) {
            return Optional.empty();
        }

        UUID uuid = null;
        if (player instanceof PlayerEntity sp) {
            uuid = sp.getUuid();
        }

        if (uuid != null) {
            Identifier catId = PLAYER_ACTIVE_CATEGORY.get(uuid);
            if (catId != null) {
                return Optional.of(catId);
            }
        }

        // Fallback for client or if map is not populated yet
        try {
            String className = SkillLevelingMod.getInstance().getPlatform().getEpicClassName(player);
            if (className != null && !className.isEmpty() && !"NONE".equals(className)) {
                return getCategoryForClass(className);
            }
        } catch (Exception e) {
            // Ignore
        }

        return Optional.empty();
    }

    /**
     * Cleans up player-specific tracking data when they disconnect.
     * Called by PlayerCleanupListener to prevent memory leaks.
     */
    public static void cleanupPlayerData(Object player) {
        if (player instanceof PlayerEntity sp) {
            PLAYER_ACTIVE_CATEGORY.remove(sp.getUuid());
        }
    }

    /**
     * Re-populates PLAYER_ACTIVE_CATEGORY from the player's stored class name
     * on login. This ensures the correct category persists across server restarts.
     */
    public static void syncOnPlayerLogin(Object player) {
        if (!enabled || !syncOnLogin || player == null) {
            return;
        }
        if (!(player instanceof PlayerEntity sp)) {
            return;
        }
        ensureCategoriesResolved();
        net.bluelotuscoding.skillleveling.util.Platform platform = SkillLevelingMod.getInstance().getPlatform();
        String className = platform.getEpicClassName(sp);
        if (className == null || className.isEmpty()
                || "NONE".equalsIgnoreCase(className)
                || "epic_classes:none".equalsIgnoreCase(className)) {
            return;
        }
        String normalized = normalizeClassName(className);
        Identifier targetCategory = RESOLVED_MAPPINGS.get(normalized);
        if (targetCategory == null) {
            java.util.List<Identifier> cats = getCategoriesForClass(className);
            if (!cats.isEmpty()) {
                targetCategory = cats.get(cats.size() - 1);
            }
        }
        if (targetCategory != null) {
            PLAYER_ACTIVE_CATEGORY.put(sp.getUuid(), targetCategory);
            int level = platform.getPufferfishLevel(sp, targetCategory);
            int xp = platform.getPufferfishExperience(sp, targetCategory);
            platform.syncEpicClassLevel(sp, level, xp, 0);
            logInfo("[Bridge] Login sync for " + sp.getName().getString()
                    + ": class=" + className + " category=" + targetCategory
                    + " level=" + level);
        }
    }

    public static void onClassChanged(Object player, String classTypeName) {
        if (!enabled || player == null || classTypeName == null) {
            return;
        }

        if (!(player instanceof PlayerEntity sp)) {
            return;
        }

        ensureCategoriesResolved();
        String normalized = normalizeClassName(classTypeName);
        if (normalized.isEmpty()) {
            return;
        }

        // 1. ALWAYS store and sync the custom name to client first.
        // This ensures UI (icons, job title) works even if category mapping fails or is
        // delayed.
        net.bluelotuscoding.skillleveling.util.Platform platform = SkillLevelingMod.getInstance().getPlatform();
        platform.setCustomClassName(sp, classTypeName);
        platform.syncCustomClassName(sp);

        if ("none".equals(normalized)) {
            PLAYER_ACTIVE_CATEGORY.remove(sp.getUuid());
            if (lockOtherCategories) {
                lockAllMappedCategories(sp);
            }
            return;
        }

        java.util.List<Identifier> categoryIds = getCategoriesForClass(classTypeName);
        if (categoryIds == null || categoryIds.isEmpty()) {
            logDebug("No mapping for class: " + normalized + " (raw: " + classTypeName + ")");
            return;
        }

        // Build the safe classes list (including parents)
        java.util.Set<String> safeClasses = new java.util.HashSet<>();
        safeClasses.add(normalized);

        net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef def = net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager
                .getClassDef(normalized);

        while (def != null && def.class_parent != null && !def.class_parent.isEmpty()) {
            safeClasses.add(def.class_parent.toLowerCase(java.util.Locale.ROOT));
            def = net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager.getClassDef(def.class_parent);
        }

        Identifier targetCategory = RESOLVED_MAPPINGS.get(normalized);
        if (targetCategory == null) {
            targetCategory = categoryIds.get(categoryIds.size() - 1);
        }
        PLAYER_ACTIVE_CATEGORY.put(sp.getUuid(), targetCategory);

        if (autoActivateCategory) {
            // Unlock ALL safe classes
            java.util.List<Identifier> safeCategoryIds = new java.util.ArrayList<>();
            for (String safeClassName : safeClasses) {
                java.util.List<Identifier> mappings = getCategoriesForClass(safeClassName);
                if (mappings != null) {
                    safeCategoryIds.addAll(mappings);
                }
            }
            unlockCategories(sp, safeCategoryIds);

            // Sync ONLY the targetCategory (the advanced class) level, NOT the sum
            int level = platform.getPufferfishLevel(sp, targetCategory);
            int xp = platform.getPufferfishExperience(sp, targetCategory);
            platform.syncEpicClassLevel(sp, level, xp, 0);
        }

        if (lockOtherCategories) {
            lockOtherMappedCategories(sp, normalized);
        }
    }

    private static void unlockCategories(Object player, java.util.List<Identifier> categoryIds) {
        if (!(player instanceof PlayerEntity sp)) {
            return;
        }

        for (Identifier categoryId : categoryIds) {
            Optional<Category> categoryOpt = SkillsAPI.getCategory(categoryId);
            if (categoryOpt.isEmpty()) {
                logWarn("Mapped category not found: " + categoryId);
                continue;
            }

            Category category = categoryOpt.get();
            if (sp instanceof ServerPlayerEntity ssp) {
                if (!category.isUnlocked(ssp)) {
                    category.unlock(ssp);
                }
            }
        }
    }

    private static void lockCategories(Object player, java.util.List<Identifier> categoryIds) {
        if (!(player instanceof PlayerEntity sp)) {
            return;
        }

        for (Identifier categoryId : categoryIds) {
            Optional<Category> categoryOpt = SkillsAPI.getCategory(categoryId);
            if (categoryOpt.isEmpty()) {
                continue;
            }

            Category category = categoryOpt.get();
            if (sp instanceof ServerPlayerEntity ssp) {
                if (category.isUnlocked(ssp)) {
                    category.lock(ssp);
                }
            }
        }
    }

    public static boolean isCategoryLocked(Object player, String categoryIdString) {
        if (!enabled || !(player instanceof PlayerEntity sp))
            return false;

        Identifier categoryId = new Identifier(categoryIdString);
        return SkillsAPI.getCategory(categoryId)
                .map((Category category) -> {
                    if (sp instanceof ServerPlayerEntity ssp) {
                        return !category.isUnlocked(ssp); // If it's NOT unlocked, it IS locked
                    }
                    return false;
                })
                .orElse(false);
    }

    private static void lockOtherMappedCategories(Object player, String selectedClassName) {
        java.util.Set<String> safeClasses = new java.util.HashSet<>();
        safeClasses.add(selectedClassName);

        net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef def = net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager
                .getClassDef(selectedClassName);

        while (def != null && def.class_parent != null && !def.class_parent.isEmpty()) {
            safeClasses.add(def.class_parent.toLowerCase(java.util.Locale.ROOT));
            def = net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager.getClassDef(def.class_parent);
        }

        for (var entry : CLASS_TO_CATEGORY.entrySet()) {
            if (!safeClasses.contains(entry.getKey())) {
                lockCategories(player, entry.getValue());
            }
        }
    }

    private static void lockAllMappedCategories(Object player) {
        for (var entry : CLASS_TO_CATEGORY.entrySet()) {
            lockCategories(player, entry.getValue());
        }
    }

    private static Identifier resolveCategoryId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }

        var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
        if (manager == null) {
            return null;
        }

        // 1. Try to find by path (most common for datapacks)
        Identifier resolved = manager.findCategoryByPath(rawId);
        if (resolved != null) {
            return resolved;
        }

        // 2. Try to interpret as a full Identifier if it has a colon
        if (rawId.contains(":")) {
            try {
                Identifier id = new Identifier(rawId);
                if (SkillsAPI.getCategory(id).isPresent()) {
                    return id;
                }
            } catch (Exception ignored) {
            }
        }

        return null; // Return null to allow retry later
    }

    private static Identifier resolveSkillId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        try {
            return new Identifier(rawId);
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeClassName(String classTypeName) {
        if (classTypeName == null) {
            return "";
        }
        String normalized = classTypeName.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("epic_classes:")) {
            return normalized.substring("epic_classes:".length());
        }
        return normalized;
    }

    public static String formatValue(String pattern, double value) {
        if (pattern == null || pattern.isEmpty()) {
            return String.valueOf(value);
        }
        try {
            java.text.DecimalFormat df = new java.text.DecimalFormat(pattern);
            return df.format(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static void logInfo(String message) {
        SkillLevelingMod.getInstance().getLogger().info(message);
    }

    private static void logWarn(String message) {
        SkillLevelingMod.getInstance().getLogger().warn(message);
    }

    private static void logDebug(String message) {
    }
}
