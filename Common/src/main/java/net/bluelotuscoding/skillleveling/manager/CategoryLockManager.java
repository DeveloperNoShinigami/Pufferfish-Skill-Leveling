package net.bluelotuscoding.skillleveling.manager;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;

import java.util.List;

/**
 * Handles server-side category locking and unlocking based on prerequisites.
 * Uses category.json prerequisite_skills parsed into LeveledConfigStorage.
 *
 * Supports "keep_unlocked" flag per category:
 * - true: Once prerequisites are met, the category stays unlocked permanently
 *   (persisted in player NBT), even if the player later loses the prerequisites.
 * - false (default): The category lock state is always re-evaluated. If
 *   prerequisites are no longer met (e.g. after a skill refund), the category
 *   becomes locked again.
 */
public final class CategoryLockManager {
    private CategoryLockManager() {
    }

    public static boolean meetsPrerequisites(ServerPlayerEntity player, Identifier categoryId) {
        List<LeveledConfigStorage.RequiredSkillEntry> prereqs = LeveledConfigStorage
                .getCategoryPrerequisites(categoryId);
        if (prereqs == null || prereqs.isEmpty()) {
            return true;
        }

        for (var req : prereqs) {
            if (!isSkillRequirementMet(player, categoryId, req)) {
                return false;
            }
        }

        return true;
    }

    public static void updateLocks(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }

        var dataManager = SkillLevelingMod.getInstance().getSkillLevelingManager().getDataManager();

        SkillsAPI.streamCategories().forEach(category -> {
            Identifier catId = category.getId();
            List<LeveledConfigStorage.RequiredSkillEntry> prereqs = LeveledConfigStorage
                    .getCategoryPrerequisites(catId);

            // Skip categories with no prerequisites
            if (prereqs == null || prereqs.isEmpty()) {
                return;
            }

            boolean meetsPrereqs = meetsPrerequisites(player, catId);
            boolean keepUnlocked = LeveledConfigStorage.isKeepUnlocked(catId);
            boolean currentlyUnlocked = category.isUnlocked(player);

            if (meetsPrereqs) {
                // Player meets prerequisites - unlock and record it
                if (!currentlyUnlocked) {
                    category.unlock(player);
                }
                // Mark as previously unlocked for keep_unlocked persistence
                dataManager.markCategoryUnlocked(player, catId);
            } else {
                // Player does NOT meet prerequisites
                if (keepUnlocked && dataManager.isCategoryPreviouslyUnlocked(player, catId)) {
                    // keep_unlocked=true and was previously unlocked: stay unlocked
                    if (!currentlyUnlocked) {
                        category.unlock(player);
                    }
                } else {
                    // keep_unlocked=false OR never unlocked before: lock it
                    if (currentlyUnlocked) {
                        category.lock(player);
                    }
                    // Clear the persisted unlock flag since prereqs aren't met
                    dataManager.markCategoryLocked(player, catId);
                }
            }
        });
    }

    public static void initializeLocks(ServerPlayerEntity player) {
        updateLocks(player);
    }

    private static boolean isSkillRequirementMet(ServerPlayerEntity player,
            Identifier categoryId,
            LeveledConfigStorage.RequiredSkillEntry req) {
        var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
        Identifier reqCategoryId = categoryId;

        if (req.categoryId != null && !req.categoryId.isEmpty()) {
            Identifier resolved = manager.findCategoryByPath(req.categoryId);
            if (resolved != null) {
                reqCategoryId = resolved;
            }
        }

        int level = manager.getTotalSkillLevel(player, reqCategoryId, req.skillId);
        return level >= req.minLevel;
    }
}
