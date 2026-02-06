package net.bluelotuscoding.skillleveling.item;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.manager.SkillLevelingManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Skill Tome - A pre-configured tome that grants a specific skill when used.
 * 
 * NBT Structure:
 * - SkillId: The skill ID to grant (e.g., "test_warrior")
 * - CategoryId: The category containing the skill (e.g., "example")
 * - LootMode: The loot mode for this skill ("both", "tome_only", "imbue_only")
 */
public class SkillTomeItem extends Item {

    public static final String NBT_SKILL_ID = "SkillId";
    public static final String NBT_CATEGORY_ID = "CategoryId";
    public static final String NBT_LOOT_MODE = "LootMode";
    public static final String NBT_LEVEL = "Level";

    // Loot mode constants
    public static final String LOOT_MODE_BOTH = "both";
    public static final String LOOT_MODE_TOME_ONLY = "tome_only";
    public static final String LOOT_MODE_IMBUE_ONLY = "imbue_only";

    public SkillTomeItem(Settings settings) {
        super(settings);
    }

    /**
     * Creates a Skill Tome ItemStack for the given skill and level.
     */
    public static ItemStack createSkillTome(Item tomeItem, String categoryId, String skillId, String lootMode,
            int level) {
        ItemStack stack = new ItemStack(tomeItem);
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(NBT_CATEGORY_ID, categoryId);
        nbt.putString(NBT_SKILL_ID, skillId);
        nbt.putString(NBT_LOOT_MODE, lootMode != null ? lootMode : LOOT_MODE_BOTH);
        nbt.putInt(NBT_LEVEL, Math.max(1, level));
        return stack;
    }

    /**
     * Creates a Skill Tome ItemStack for the given skill (default level 1).
     */
    public static ItemStack createSkillTome(Item tomeItem, String categoryId, String skillId, String lootMode) {
        return createSkillTome(tomeItem, categoryId, skillId, lootMode, 1);
    }

    /**
     * Creates a Skill Tome with default loot mode (default level 1).
     */
    public static ItemStack createSkillTome(Item tomeItem, String categoryId, String skillId) {
        return createSkillTome(tomeItem, categoryId, skillId, LOOT_MODE_BOTH, 1);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return TypedActionResult.pass(stack);
        }

        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_SKILL_ID) || !nbt.contains(NBT_CATEGORY_ID)) {
            serverPlayer.sendMessage(Text.translatable("skillleveling.tome.invalid"), false);
            return TypedActionResult.fail(stack);
        }

        String skillId = nbt.getString(NBT_SKILL_ID);
        String categoryId = nbt.getString(NBT_CATEGORY_ID);
        String lootMode = nbt.getString(NBT_LOOT_MODE);
        int tomeLevel = nbt.contains(NBT_LEVEL) ? nbt.getInt(NBT_LEVEL) : 1;

        // Ensure this skill is actually supposed to be lootable
        var config = net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.get(skillId);
        if (config == null || config.lootMode == null) {
            serverPlayer.sendMessage(Text.translatable("skillleveling.tome.unsupported_skill"), false);
            return TypedActionResult.fail(stack);
        }

        // Check if this is an "imbue_only" tome - can't be used directly
        if (LOOT_MODE_IMBUE_ONLY.equals(lootMode)) {
            serverPlayer.sendMessage(Text.translatable("skillleveling.tome.imbue_required"), false);
            return TypedActionResult.fail(stack);
        }

        // Get skill manager and try to grant the skill
        SkillLevelingManager manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
        if (manager == null) {
            serverPlayer.sendMessage(Text.translatable("skillleveling.tome.not_initialized"), false);
            return TypedActionResult.fail(stack);
        }

        // Create identifier for category - use manager's normalizeCategoryId to resolve
        // the actual registered category (handles cases where categoryId is just the
        // path
        // like "additional_skills" instead of full
        // "skillleveling_test:additional_skills")
        Identifier rawCatId = new Identifier(categoryId);
        Identifier catId = manager.normalizeCategoryId(rawCatId);

        SkillLevelingMod.getInstance().getLogger()
                .debug("[SkillTome] Resolving category: raw=" + rawCatId + " -> normalized=" + catId);

        // Check current BASE level for tome usage
        int currentLevel = manager.getBaseSkillLevel(serverPlayer, catId, skillId);
        int maxLevel = manager.getMaxLevel(catId, skillId);

        if (maxLevel <= 0) {
            // Skill not found or doesn't support leveling
            serverPlayer.sendMessage(Text.translatable("skillleveling.tome.not_found", skillId,
                    net.bluelotuscoding.skillleveling.util.CategoryTitleHelper.getCategoryTitle(categoryId)), false);
            return TypedActionResult.fail(stack);
        }

        if (currentLevel >= tomeLevel) {
            serverPlayer.sendMessage(Text.translatable("skillleveling.tome.already_at_level", tomeLevel), false);
            return TypedActionResult.fail(stack);
        }

        // Grant the specific skill level - Bypass points for tome usage
        boolean success = manager.setSkillLevel(serverPlayer, catId, skillId, tomeLevel, true);

        if (success) {
            String skillName = getSkillDisplayName(skillId);
            serverPlayer.sendMessage(Text.translatable("skillleveling.tome.learn_success", skillName, tomeLevel),
                    false);

            // Consume the tome
            if (!serverPlayer.getAbilities().creativeMode) {
                stack.decrement(1);
            }

            return TypedActionResult.success(stack);
        } else {
            serverPlayer.sendMessage(Text.translatable("skillleveling.tome.learn_fail"), false);
            return TypedActionResult.fail(stack);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_SKILL_ID)) {
            tooltip.add(Text.translatable("item.puffish_skill_leveling.skill_tome.unconfigured")
                    .formatted(Formatting.RED, Formatting.ITALIC));
            return;
        }

        String skillId = nbt.getString(NBT_SKILL_ID);
        String categoryId = nbt.getString(NBT_CATEGORY_ID);
        String lootMode = nbt.getString(NBT_LOOT_MODE);
        int level = nbt.contains(NBT_LEVEL) ? nbt.getInt(NBT_LEVEL) : 1;

        // Skill name and level
        String skillName = getSkillDisplayName(skillId);
        tooltip.add(Text.translatable("item.puffish_skill_leveling.skill_tome.desc1",
                Text.literal(skillName + " (+" + level + ")").formatted(Formatting.BLUE)));

        // Category (fetch title from datapack via helper)
        tooltip.add(Text.translatable("skillleveling.tome.category",
                net.bluelotuscoding.skillleveling.util.CategoryTitleHelper.getCategoryTitle(categoryId))
                .formatted(Formatting.DARK_GRAY));

        // Loot mode indicator
        if (LOOT_MODE_IMBUE_ONLY.equals(lootMode)) {
            tooltip.add(
                    Text.translatable("item.puffish_skill_leveling.skill_tome.imbue_only").formatted(Formatting.RED));
        } else if (LOOT_MODE_BOTH.equals(lootMode)) {
            tooltip.add(
                    Text.translatable("item.puffish_skill_leveling.skill_tome.both_mode").formatted(Formatting.GOLD));
            tooltip.add(Text.translatable("item.puffish_skill_leveling.skill_tome.use_hint").formatted(
                    Formatting.DARK_PURPLE,
                    Formatting.ITALIC));
        } else {
            tooltip.add(Text.translatable("item.puffish_skill_leveling.skill_tome.use_hint").formatted(
                    Formatting.DARK_PURPLE,
                    Formatting.ITALIC));
        }
    }

    /**
     * Get a display-friendly name for a skill ID.
     * Converts snake_case to Title Case.
     */
    private static String getSkillDisplayName(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return "Unknown";
        }
        // Convert snake_case to Title Case
        String[] parts = skillId.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    /**
     * Get the skill ID from a Skill Tome stack.
     */
    public static String getSkillId(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getString(NBT_SKILL_ID) : null;
    }

    /**
     * Get the category ID from a Skill Tome stack.
     */
    public static String getCategoryId(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getString(NBT_CATEGORY_ID) : null;
    }

    /**
     * Get the loot mode from a Skill Tome stack.
     */
    public static String getLootMode(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_LOOT_MODE)) {
            return LOOT_MODE_BOTH;
        }
        String mode = nbt.getString(NBT_LOOT_MODE);
        return (mode == null || mode.isEmpty()) ? LOOT_MODE_BOTH : mode;
    }

    /**
     * Get the level from a Skill Tome stack.
     */
    public static int getLevel(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(NBT_LEVEL) ? nbt.getInt(NBT_LEVEL) : 1;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        // Skill Tomes have the enchantment glint effect
        return true;
    }
}
