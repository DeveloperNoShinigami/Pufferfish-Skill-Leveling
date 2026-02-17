package net.bluelotuscoding.skillleveling.bridge;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.SkillsAPI;

public final class EpicClassBridge {
    private static final Map<String, Identifier> CLASS_TO_CATEGORY = new HashMap<>();
    private static final Map<Identifier, String> CATEGORY_TO_CLASS = new HashMap<>();
    private static final Map<String, String> RAW_CLASS_MAPPINGS = new HashMap<>();

    private static boolean enabled = false;
    private static boolean autoActivateCategory = true;
    private static boolean lockOtherCategories = true;
    private static boolean syncOnLogin = true;
    private static boolean categoriesResolved = false;

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
        categoriesResolved = false;

        // Store raw mappings for later resolution
        for (var entry : config.classToCategoryMap.entrySet()) {
            String className = normalizeClassName(entry.getKey());
            if (!className.isEmpty() && entry.getValue() != null && !entry.getValue().isBlank()) {
                RAW_CLASS_MAPPINGS.put(className, entry.getValue());
            }
        }

        logInfo("Epic Class bridge config loaded (" + RAW_CLASS_MAPPINGS.size() + " mappings)");
    }

    /**
     * Resolves category IDs from raw string mappings.
     * Called lazily when first needed after Pufferfish Skills is initialized.
     */
    private static void ensureCategoriesResolved() {
        if (categoriesResolved || RAW_CLASS_MAPPINGS.isEmpty()) {
            return;
        }

        // Check if Skills API is ready
        try {
            var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
            if (manager == null) {
                return; // Not ready yet
            }
        } catch (Exception e) {
            return; // Not ready yet
        }

        CLASS_TO_CATEGORY.clear();
        CATEGORY_TO_CLASS.clear();

        for (var entry : RAW_CLASS_MAPPINGS.entrySet()) {
            String className = entry.getKey();
            Identifier categoryId = resolveCategoryId(entry.getValue());
            if (categoryId == null) {
                logWarn("Could not resolve category for class " + className + ": " + entry.getValue());
                continue;
            }

            CLASS_TO_CATEGORY.put(className, categoryId);
            CATEGORY_TO_CLASS.put(categoryId, className);
        }

        categoriesResolved = true;
        logInfo("Epic Class bridge mappings resolved: " + CLASS_TO_CATEGORY.size() + " categories");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean shouldSyncOnLogin() {
        return syncOnLogin;
    }

    public static Optional<String> getClassForCategory(Identifier categoryId) {
        ensureCategoriesResolved();
        return Optional.ofNullable(CATEGORY_TO_CLASS.get(categoryId));
    }

    public static Optional<Identifier> getCategoryForClass(String classTypeName) {
        ensureCategoriesResolved();
        String normalized = normalizeClassName(classTypeName);
        return Optional.ofNullable(CLASS_TO_CATEGORY.get(normalized));
    }

    public static void onClassChanged(ServerPlayerEntity player, String classTypeName) {
        if (!enabled || player == null || classTypeName == null) {
            return;
        }

        ensureCategoriesResolved();
        String normalized = normalizeClassName(classTypeName);
        if (normalized.isEmpty()) {
            return;
        }

        if ("NONE".equals(normalized)) {
            if (lockOtherCategories) {
                lockAllMappedCategories(player);
            }
            return;
        }

        Identifier categoryId = CLASS_TO_CATEGORY.get(normalized);
        if (categoryId == null) {
            logDebug("No mapping for class: " + normalized);
            return;
        }

        if (autoActivateCategory) {
            unlockCategory(player, categoryId);
        }

        if (lockOtherCategories) {
            lockOtherMappedCategories(player, normalized);
        }
    }

    private static void unlockCategory(ServerPlayerEntity player, Identifier categoryId) {
        Optional<Category> categoryOpt = SkillsAPI.getCategory(categoryId);
        if (categoryOpt.isEmpty()) {
            logWarn("Mapped category not found: " + categoryId);
            return;
        }

        Category category = categoryOpt.get();
        if (!category.isUnlocked(player)) {
            category.unlock(player);
        }
    }

    private static void lockCategory(ServerPlayerEntity player, Identifier categoryId) {
        Optional<Category> categoryOpt = SkillsAPI.getCategory(categoryId);
        if (categoryOpt.isEmpty()) {
            return;
        }

        Category category = categoryOpt.get();
        if (category.isUnlocked(player)) {
            category.lock(player);
        }
    }

    private static void lockOtherMappedCategories(ServerPlayerEntity player, String selectedClassName) {
        for (var entry : CLASS_TO_CATEGORY.entrySet()) {
            if (!entry.getKey().equals(selectedClassName)) {
                lockCategory(player, entry.getValue());
            }
        }
    }

    private static void lockAllMappedCategories(ServerPlayerEntity player) {
        for (var entry : CLASS_TO_CATEGORY.entrySet()) {
            lockCategory(player, entry.getValue());
        }
    }

    private static Identifier resolveCategoryId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }

        var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
        Identifier resolved = manager.findCategoryByPath(rawId);
        if (resolved != null) {
            return resolved;
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
        return classTypeName.trim().toUpperCase(Locale.ROOT);
    }

    private static void logInfo(String message) {
        SkillLevelingMod.getInstance().getLogger().info(message);
    }

    private static void logWarn(String message) {
        SkillLevelingMod.getInstance().getLogger().warn(message);
    }

    private static void logDebug(String message) {
        SkillLevelingMod.getInstance().getLogger().debug(message);
    }
}
