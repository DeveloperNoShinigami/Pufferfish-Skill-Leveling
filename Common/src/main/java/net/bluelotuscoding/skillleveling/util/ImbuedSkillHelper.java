package net.bluelotuscoding.skillleveling.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for reading and writing multi-skill imbued data on ItemStacks.
 * 
 * NBT Structure:
 * {
 * "SkillLevelingImbued": {
 * "slots": 2,
 * "skills": [
 * {"categoryId": "...", "skillId": "vitality", "level": 2},
 * {"categoryId": "...", "skillId": "berserker", "level": 1}
 * ]
 * }
 * }
 */
public class ImbuedSkillHelper {

    public static final String NBT_ROOT = "SkillLevelingImbued";
    public static final String NBT_SLOTS = "slots";
    public static final String NBT_SKILLS = "skills";
    public static final String NBT_CATEGORY_ID = "categoryId";
    public static final String NBT_SKILL_ID = "skillId";
    public static final String NBT_LEVEL = "level";
    public static final int MAX_SLOTS = 3;

    /**
     * Represents a single imbued skill on an item.
     */
    public static class ImbuedSkill {
        public final String categoryId;
        public final String skillId;
        public final int level;

        public ImbuedSkill(String categoryId, String skillId, int level) {
            this.categoryId = categoryId;
            this.skillId = skillId;
            this.level = level;
        }

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putString(NBT_CATEGORY_ID, categoryId);
            nbt.putString(NBT_SKILL_ID, skillId);
            nbt.putInt(NBT_LEVEL, level);
            return nbt;
        }

        public static ImbuedSkill fromNbt(NbtCompound nbt) {
            return new ImbuedSkill(
                    nbt.getString(NBT_CATEGORY_ID),
                    nbt.getString(NBT_SKILL_ID),
                    nbt.getInt(NBT_LEVEL));
        }
    }

    /**
     * Gets the number of open skill slots on an item.
     */
    public static int getSlotCount(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasNbt()) {
            return 0;
        }
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ROOT)) {
            return 0;
        }
        return nbt.getCompound(NBT_ROOT).getInt(NBT_SLOTS);
    }

    /**
     * Sets the number of open skill slots on an item.
     */
    public static void setSlotCount(ItemStack stack, int slots) {
        NbtCompound nbt = stack.getOrCreateNbt();
        NbtCompound root = nbt.contains(NBT_ROOT) ? nbt.getCompound(NBT_ROOT) : new NbtCompound();
        root.putInt(NBT_SLOTS, Math.min(slots, MAX_SLOTS));
        nbt.put(NBT_ROOT, root);
    }

    /**
     * Gets all imbued skills on an item.
     */
    public static List<ImbuedSkill> getSkills(ItemStack stack) {
        List<ImbuedSkill> skills = new ArrayList<>();
        if (stack.isEmpty() || !stack.hasNbt()) {
            return skills;
        }
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ROOT)) {
            return skills;
        }
        NbtCompound root = nbt.getCompound(NBT_ROOT);
        if (!root.contains(NBT_SKILLS)) {
            return skills;
        }
        NbtList skillList = root.getList(NBT_SKILLS, 10); // 10 = NbtCompound type
        for (int i = 0; i < skillList.size(); i++) {
            skills.add(ImbuedSkill.fromNbt(skillList.getCompound(i)));
        }
        return skills;
    }

    /**
     * Adds a skill to the next available slot.
     * Returns true if successful, false if no slots available.
     */
    public static boolean addSkill(ItemStack stack, String categoryId, String skillId, int level) {
        int slots = getSlotCount(stack);
        List<ImbuedSkill> skills = getSkills(stack);

        if (skills.size() >= slots) {
            return false; // No empty slots
        }

        skills.add(new ImbuedSkill(categoryId, skillId, level));
        saveSkills(stack, skills);
        return true;
    }

    /**
     * Removes a skill by skillId.
     * Returns the removed skill data, or null if not found.
     */
    public static ImbuedSkill removeSkill(ItemStack stack, String skillId) {
        List<ImbuedSkill> skills = getSkills(stack);
        ImbuedSkill removed = null;

        for (int i = 0; i < skills.size(); i++) {
            if (skills.get(i).skillId.equals(skillId)) {
                removed = skills.remove(i);
                break;
            }
        }

        if (removed != null) {
            saveSkills(stack, skills);
        }
        return removed;
    }

    /**
     * Upgrades a skill's level by 1.
     * Returns true if successful, false if skill not found.
     */
    public static boolean upgradeSkill(ItemStack stack, String skillId) {
        List<ImbuedSkill> skills = getSkills(stack);
        boolean upgraded = false;

        for (int i = 0; i < skills.size(); i++) {
            ImbuedSkill skill = skills.get(i);
            if (skill.skillId.equals(skillId)) {
                skills.set(i, new ImbuedSkill(skill.categoryId, skill.skillId, skill.level + 1));
                upgraded = true;
                break;
            }
        }

        if (upgraded) {
            saveSkills(stack, skills);
        }
        return upgraded;
    }

    /**
     * Checks if an item has a specific skill.
     */
    public static boolean hasSkill(ItemStack stack, String skillId) {
        return getSkills(stack).stream().anyMatch(s -> s.skillId.equals(skillId));
    }

    /**
     * Gets the level of a specific skill on an item.
     * Returns 0 if skill not found.
     */
    public static int getSkillLevel(ItemStack stack, String skillId) {
        return getSkills(stack).stream()
                .filter(s -> s.skillId.equals(skillId))
                .findFirst()
                .map(s -> s.level)
                .orElse(0);
    }

    /**
     * Checks if an item can accept more skills (has empty slots).
     */
    public static boolean hasEmptySlot(ItemStack stack) {
        return getSkills(stack).size() < getSlotCount(stack);
    }

    /**
     * Checks if an item can accept more slot openings.
     */
    public static boolean canOpenMoreSlots(ItemStack stack) {
        return getSlotCount(stack) < MAX_SLOTS;
    }

    /**
     * Saves the skill list to the item's NBT.
     */
    private static void saveSkills(ItemStack stack, List<ImbuedSkill> skills) {
        NbtCompound nbt = stack.getOrCreateNbt();
        NbtCompound root = nbt.contains(NBT_ROOT) ? nbt.getCompound(NBT_ROOT) : new NbtCompound();

        NbtList skillList = new NbtList();
        for (ImbuedSkill skill : skills) {
            skillList.add(skill.toNbt());
        }
        root.put(NBT_SKILLS, skillList);
        nbt.put(NBT_ROOT, root);
    }

    /**
     * Migrates old single-skill NBT format to new multi-skill format.
     * Old format: { "SkillLevelingImbued": { "CategoryId": "...", "SkillId": "...",
     * "Level": N } }
     * New format: { "SkillLevelingImbued": { "slots": 1, "skills": [...] } }
     */
    public static void migrateOldFormat(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasNbt()) {
            return;
        }
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ROOT)) {
            return;
        }
        NbtCompound root = nbt.getCompound(NBT_ROOT);

        // Check for old format (has "CategoryId" instead of "slots")
        if (root.contains("CategoryId") && !root.contains(NBT_SLOTS)) {
            String categoryId = root.getString("CategoryId");
            String skillId = root.getString("SkillId");
            int level = root.getInt("Level");

            // Convert to new format
            NbtCompound newRoot = new NbtCompound();
            newRoot.putInt(NBT_SLOTS, 1);

            NbtList skillList = new NbtList();
            NbtCompound skillNbt = new NbtCompound();
            skillNbt.putString(NBT_CATEGORY_ID, categoryId);
            skillNbt.putString(NBT_SKILL_ID, skillId);
            skillNbt.putInt(NBT_LEVEL, level);
            skillList.add(skillNbt);

            newRoot.put(NBT_SKILLS, skillList);
            nbt.put(NBT_ROOT, newRoot);
        }
    }
}
