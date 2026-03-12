package net.bluelotuscoding.skillleveling.tome;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.item.TomeItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages pending Tome actions that require player input via chat.
 * 
 * Flow:
 * 1. Player uses Tome → Creates PendingTomeAction
 * 2. Server prompts player to enter category/skill name
 * 3. Player types in chat → ServerChatMixin intercepts
 * 4. Validate input → Process action or re-prompt
 */
public class TomePendingActionManager {

    /**
     * Represents a pending tome action awaiting player input.
     */
    public static class PendingTomeAction {
        public final TomeItem.TomeType tomeType;
        public final long createdAt;
        public final Identifier categoryId; // null until category is selected
        public final String skillId; // null until skill is selected
        public final int stage; // 0 = category, 1 = skill, 2 = amount (Greater Clear), 3 = quantity (Exp Tome)

        // Exp Tome specific fields
        public final int xpAmount;
        public final String tomeName;
        public final int totalSameTomes;

        public PendingTomeAction(TomeItem.TomeType tomeType, Identifier categoryId, String skillId, int stage,
                int xpAmount, String tomeName, int totalSameTomes) {
            this.tomeType = tomeType;
            this.categoryId = categoryId;
            this.skillId = skillId;
            this.stage = stage;
            this.xpAmount = xpAmount;
            this.tomeName = tomeName;
            this.totalSameTomes = totalSameTomes;
            this.createdAt = System.currentTimeMillis();
        }

        public PendingTomeAction withCategory(Identifier categoryId) {
            return new PendingTomeAction(tomeType, categoryId, null, 1, xpAmount, tomeName, totalSameTomes);
        }

        public PendingTomeAction withSkill(String skillId) {
            return new PendingTomeAction(tomeType, categoryId, skillId, 2, xpAmount, tomeName, totalSameTomes);
        }

        public PendingTomeAction withQuantityStage(Identifier categoryId, int totalSameTomes) {
            return new PendingTomeAction(tomeType, categoryId, null, 3, xpAmount, tomeName, totalSameTomes);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > 60_000; // 60 second timeout
        }
    }

    private static final Map<UUID, PendingTomeAction> pendingActions = new ConcurrentHashMap<>();

    /**
     * Convert snake_case ID to Title Case for display.
     * Example: "test_warrior" → "Test Warrior"
     */
    private static String toTitleCase(String id) {
        if (id == null || id.isEmpty()) {
            return id;
        }
        String[] words = id.split("_");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }
        return result.toString();
    }

    /**
     * Start a tome action for a player.
     */
    public static void startTomeAction(ServerPlayerEntity player, TomeItem.TomeType tomeType) {
        UUID playerId = player.getUuid();

        // Clear any existing pending action
        pendingActions.remove(playerId);

        int xpAmount = 0;
        String tomeName = "";
        if (tomeType == TomeItem.TomeType.EXPERIENCE) {
            net.minecraft.item.ItemStack stack = player.getMainHandStack();
            if (!(stack.getItem() instanceof net.bluelotuscoding.skillleveling.item.ExpTomeItem)) {
                stack = player.getOffHandStack();
            }
            if (stack.getItem() instanceof net.bluelotuscoding.skillleveling.item.ExpTomeItem) {
                xpAmount = net.bluelotuscoding.skillleveling.item.ExpTomeItem.getExperienceGranted(stack);
                tomeName = stack.getName().getString();
            }
        }

        // All tomes start with category selection
        pendingActions.put(playerId, new PendingTomeAction(tomeType, null, null, 0, xpAmount, tomeName, 0));
        sendCategoryPrompt(player);
    }

    /**
     * Check if a player has a pending action.
     */
    public static boolean hasPendingAction(ServerPlayerEntity player) {
        var action = pendingActions.get(player.getUuid());
        if (action != null && action.isExpired()) {
            pendingActions.remove(player.getUuid());
            return false;
        }
        return action != null;
    }

    /**
     * Get the pending action for a player.
     */
    public static Optional<PendingTomeAction> getPendingAction(ServerPlayerEntity player) {
        var action = pendingActions.get(player.getUuid());
        if (action != null && action.isExpired()) {
            pendingActions.remove(player.getUuid());
            player.sendMessage(Text.literal("§cTome action timed out."), false);
            return Optional.empty();
        }
        return Optional.ofNullable(action);
    }

    /**
     * Process player chat input for pending tome action.
     * Returns true if the message was consumed (was a tome response).
     */
    public static boolean processPlayerInput(ServerPlayerEntity player, String message) {
        var optAction = getPendingAction(player);
        if (optAction.isEmpty()) {
            return false;
        }

        var action = optAction.get();
        String input = message.trim().toLowerCase();

        // Handle cancel
        if (input.equals("cancel")) {
            pendingActions.remove(player.getUuid());
            player.sendMessage(Text.literal("§eTome action cancelled."), false);
            return true;
        }

        switch (action.stage) {
            case 0: // Awaiting category
                return processCategoryInput(player, action, input);
            case 1: // Awaiting skill
                return processSkillInput(player, action, input);
            case 2: // Awaiting amount (Greater Clear Mind only)
                return processAmountInput(player, action, input);
            case 3: // Awaiting quantity (Exp Tome bulk consume)
                return processQuantityInput(player, action, input);
            default:
                return false;
        }
    }

    private static boolean processCategoryInput(ServerPlayerEntity player, PendingTomeAction action, String input) {
        // Try to find a matching category (by ID path or title case)
        List<Category> categories = SkillsAPI.streamCategories().collect(Collectors.toList());
        Identifier matchedCategory = null;

        for (var category : categories) {
            String categoryId = category.getId().getPath().toLowerCase();
            String categoryTitle = toTitleCase(category.getId().getPath()).toLowerCase();

            if (categoryId.equals(input)
                    || categoryTitle.equals(input)
                    || category.getId().toString().toLowerCase().equals(input)) {
                matchedCategory = category.getId();
                break;
            }
        }

        if (matchedCategory == null) {
            player.sendMessage(Text.literal("§cCategory '" + input + "' not found. Try again or type 'cancel'."),
                    false);
            sendCategoryPrompt(player);
            return true;
        }

        // Category found
        if (action.tomeType == TomeItem.TomeType.PROGRESSION) {
            // Progression tome - directly apply
            boolean success = SkillLevelingMod.getInstance().getSkillLevelingManager()
                    .handleTomeOfProgression(player, matchedCategory);
            if (success) {
                consumeTome(player, action.tomeType, 1);
                player.sendMessage(Text.literal("§a+1 skill point in " + toTitleCase(matchedCategory.getPath()) + "!"),
                        false);
            } else {
                player.sendMessage(Text.literal("§cFailed to add point. Category may be at max level."), false);
            }
            pendingActions.remove(player.getUuid());
        } else if (action.tomeType == TomeItem.TomeType.EXPERIENCE) {
            // Experience tome - check for bulk consumption
            int totalIdentical = countIdenticalTomes(player);
            if (totalIdentical > 1) {
                pendingActions.put(player.getUuid(), action.withQuantityStage(matchedCategory, totalIdentical));
                player.sendMessage(
                        Text.literal("§eYou have §f" + totalIdentical + "§e of these tomes in your inventory."), false);
                player.sendMessage(
                        Text.literal("§eHow many would you like to consume? (1-" + totalIdentical + ", or 'all')"),
                        false);
            } else {
                // Only 1 tome, process immediately
                applyExperience(player, matchedCategory, action.xpAmount, 1);
                pendingActions.remove(player.getUuid());
            }
        } else {
            // Clear Mind tomes - need skill selection next
            pendingActions.put(player.getUuid(), action.withCategory(matchedCategory));
            sendSkillPrompt(player, matchedCategory);
        }
        return true;
    }

    private static boolean processSkillInput(ServerPlayerEntity player, PendingTomeAction action, String input) {
        // Try to find a matching skill in the category
        var categoryOpt = SkillsAPI.getCategory(action.categoryId);
        if (categoryOpt.isEmpty()) {
            player.sendMessage(Text.literal("§cError: Category no longer exists."), false);
            pendingActions.remove(player.getUuid());
            return true;
        }

        var category = categoryOpt.get();
        String matchedSkillId = null;

        List<Skill> skills = category.streamSkills().collect(Collectors.toList());
        for (var skill : skills) {
            String skillId = skill.getId().toLowerCase();
            String skillTitle = toTitleCase(skill.getId()).toLowerCase();

            if (skillId.equals(input) || skillTitle.equals(input)) {
                matchedSkillId = skill.getId();
                break;
            }
        }

        if (matchedSkillId == null) {
            player.sendMessage(Text.literal("§cSkill '" + input + "' not found in "
                    + toTitleCase(action.categoryId.getPath()) + ". Try again or type 'cancel'."), false);
            sendSkillPrompt(player, action.categoryId);
            return true;
        }

        // Check if player has the skill unlocked (use base level for gear-independent
        // refund check)
        int currentLevel = SkillLevelingMod.getInstance().getBaseSkillLevel(player, action.categoryId, matchedSkillId);
        SkillLevelingMod.getInstance().getLogger()
                .info("[TomeAction] Detected level " + currentLevel + " for skill " + matchedSkillId + " (canonical)");
        if (currentLevel <= 0) {
            player.sendMessage(
                    Text.literal("§cYou don't have any levels in skill '" + toTitleCase(matchedSkillId) + "'."), false);
            pendingActions.remove(player.getUuid());
            return true;
        }

        if (action.tomeType == TomeItem.TomeType.CLEAR_MIND) {
            // Clear Mind - refund 1 level
            boolean success = SkillLevelingMod.getInstance().getSkillLevelingManager()
                    .handleTomeOfClearMind(player, action.categoryId, matchedSkillId);
            if (success) {
                consumeTome(player, action.tomeType, 1);
                player.sendMessage(Text.literal("§aRefunded 1 level of " + toTitleCase(matchedSkillId) + "!"), false);
            } else {
                player.sendMessage(Text.literal("§cFailed to refund. Skill may already be at level 0."), false);
            }
            pendingActions.remove(player.getUuid());
        } else if (action.tomeType == TomeItem.TomeType.GREATER_CLEAR_MIND) {
            // Greater Clear Mind - ask for amount
            pendingActions.put(player.getUuid(), action.withSkill(matchedSkillId));
            player.sendMessage(Text.literal("§eHow many levels to refund? (1-" + currentLevel + ", or 'all')"), false);
        }
        return true;
    }

    private static boolean processAmountInput(ServerPlayerEntity player, PendingTomeAction action, String input) {
        int currentLevel = SkillLevelingMod.getInstance().getBaseSkillLevel(player, action.categoryId, action.skillId);

        int amount;
        if (input.equals("all")) {
            amount = currentLevel;
        } else {
            try {
                amount = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                player.sendMessage(
                        Text.literal(
                                "§cPlease enter a number (1-" + currentLevel + ") or 'all'. Type 'cancel' to cancel."),
                        false);
                return true;
            }
        }

        if (amount < 1 || amount > currentLevel) {
            player.sendMessage(Text.literal("§cInvalid amount. Please enter 1-" + currentLevel + " or 'all'."), false);
            return true;
        }

        // ATOMIC CHECK: Verify if refunding 'amount' levels is possible
        // Calculate target level: currentLevel - amount
        int targetLevel = currentLevel - amount;
        var blockers = SkillLevelingMod.getInstance().getSkillLevelingManager()
                .checkPrerequisites(player, action.categoryId, action.skillId, targetLevel);

        if (!blockers.isEmpty()) {
            String joined = String.join(", ", blockers);
            player.sendMessage(Text.translatable("skillleveling.refund.blocked_by_prereq", joined)
                    .formatted(net.minecraft.util.Formatting.RED), false);
            return true;
        }

        // Refund the specified amount
        int refunded = 0;
        int totalPoints = 0;
        var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
        for (int i = 0; i < amount; i++) {
            // Calculate points for the level being refunded
            int levelToRefund = currentLevel - i;
            int points = manager.getPointsForLevel(action.categoryId, action.skillId, levelToRefund);

            if (manager.refundSkillLevel(player, action.categoryId, action.skillId)) {
                refunded++;
                totalPoints += points;
            } else {
                break;
            }
        }

        if (refunded > 0) {
            // Explicitly sync points ONCE after all refunds are complete with message
            manager.syncCategoryPoints(player, action.categoryId, totalPoints);

            consumeTome(player, action.tomeType, 1);
            player.sendMessage(
                    Text.literal("§aRefunded " + refunded + " level(s) of " + toTitleCase(action.skillId) + "!"),
                    false);
        } else {
            player.sendMessage(Text.literal("§cFailed to refund levels."), false);
        }
        pendingActions.remove(player.getUuid());
        return true;
    }

    private static boolean processQuantityInput(ServerPlayerEntity player, PendingTomeAction action, String input) {
        int amount;
        if (input.equals("all")) {
            amount = action.totalSameTomes;
        } else {
            try {
                amount = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                player.sendMessage(
                        Text.literal("§cPlease enter a number (1-" + action.totalSameTomes + ") or 'all'."),
                        false);
                return true;
            }
        }

        if (amount < 1 || amount > action.totalSameTomes) {
            player.sendMessage(Text.literal("§cInvalid amount. Please enter 1-" + action.totalSameTomes + " or 'all'."),
                    false);
            return true;
        }

        applyExperience(player, action.categoryId, action.xpAmount, amount);
        pendingActions.remove(player.getUuid());
        return true;
    }

    private static void applyExperience(ServerPlayerEntity player, Identifier categoryId, int xpPerTome, int quantity) {
        int totalXp = xpPerTome * quantity;
        SkillLevelingMod.getInstance().getPlatform().addPufferfishExperience(player, categoryId, totalXp);
        consumeTome(player, TomeItem.TomeType.EXPERIENCE, quantity);
        player.sendMessage(Text.literal("§aConsumed §f" + quantity + " " + toTitleCase(categoryId.getPath())
                + " Experience Tomes§a for §f" + totalXp + " XP§a!"), false);
    }

    private static int countIdenticalTomes(ServerPlayerEntity player) {
        net.minecraft.item.ItemStack source = player.getMainHandStack();
        if (!(source.getItem() instanceof net.bluelotuscoding.skillleveling.item.ExpTomeItem)) {
            source = player.getOffHandStack();
        }
        if (!(source.getItem() instanceof net.bluelotuscoding.skillleveling.item.ExpTomeItem))
            return 0;

        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            net.minecraft.item.ItemStack stack = player.getInventory().getStack(i);
            if (net.minecraft.item.ItemStack.canCombine(source, stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void sendCategoryPrompt(ServerPlayerEntity player) {
        List<Category> categories = SkillsAPI.streamCategories().collect(Collectors.toList());
        StringBuilder sb = new StringBuilder("§eAvailable categories: ");
        boolean first = true;
        for (var category : categories) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("§f").append(toTitleCase(category.getId().getPath()));
            first = false;
        }
        player.sendMessage(Text.literal(sb.toString()), false);
        player.sendMessage(Text.literal("§eType the category name (or 'cancel' to cancel):"), false);
    }

    private static void sendSkillPrompt(ServerPlayerEntity player, Identifier categoryId) {
        var categoryOpt = SkillsAPI.getCategory(categoryId);
        if (categoryOpt.isEmpty()) {
            player.sendMessage(Text.literal("§cError: Category not found."), false);
            return;
        }

        var category = categoryOpt.get();
        List<Skill> skills = category.streamSkills().collect(Collectors.toList());
        StringBuilder sb = new StringBuilder("§eAvailable skills in " + toTitleCase(categoryId.getPath()) + ": ");
        boolean first = true;
        for (var skill : skills) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("§f").append(toTitleCase(skill.getId()));
            first = false;
        }
        player.sendMessage(Text.literal(sb.toString()), false);
        player.sendMessage(Text.literal("§eType the skill name (or 'cancel' to cancel):"), false);
    }

    private static void consumeTome(ServerPlayerEntity player, TomeItem.TomeType tomeType, int amount) {
        if (tomeType == TomeItem.TomeType.EXPERIENCE) {
            // Bulk consume across inventory
            net.minecraft.item.ItemStack source = player.getMainHandStack();
            if (!(source.getItem() instanceof net.bluelotuscoding.skillleveling.item.ExpTomeItem)) {
                source = player.getOffHandStack();
            }
            if (!(source.getItem() instanceof net.bluelotuscoding.skillleveling.item.ExpTomeItem))
                return;

            int remaining = amount;
            for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
                net.minecraft.item.ItemStack stack = player.getInventory().getStack(i);
                if (net.minecraft.item.ItemStack.canCombine(source, stack)) {
                    int toTake = Math.min(stack.getCount(), remaining);
                    stack.decrement(toTake);
                    remaining -= toTake;
                }
            }
        } else {
            // Single tome consume from hand
            var mainHand = player.getMainHandStack();
            var offHand = player.getOffHandStack();

            if (mainHand.getItem() instanceof TomeItem tome && tome.getTomeType() == tomeType) {
                if (!player.getAbilities().creativeMode)
                    mainHand.decrement(1);
            } else if (offHand.getItem() instanceof TomeItem tome && tome.getTomeType() == tomeType) {
                if (!player.getAbilities().creativeMode)
                    offHand.decrement(1);
            }
        }
    }

    /**
     * Clear expired actions (call periodically or on player disconnect).
     */
    public static void clearPlayerAction(ServerPlayerEntity player) {
        pendingActions.remove(player.getUuid());
    }
}
