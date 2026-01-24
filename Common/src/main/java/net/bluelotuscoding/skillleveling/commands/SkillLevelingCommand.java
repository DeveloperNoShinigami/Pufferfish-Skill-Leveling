package net.bluelotuscoding.skillleveling.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.mixin_interface.CategoryDataExtension;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.server.data.CategoryData;
import net.puffish.skillsmod.server.data.PlayerData;

/**
 * Simplified commands for managing skill levels in the addon.
 * These commands COMPLEMENT the base skill mod, not replace it.
 * 
 * Commands:
 * - /skillleveling get <player> <category> <skill> - View skill level
 * - /skillleveling set <player> <category> <skill> <level> - Set level directly
 * - /skillleveling refund <player> <category> <skill> [amount|all] - Refund
 * levels
 */
public class SkillLevelingCommand {

        public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
                dispatcher.register(
                                CommandManager.literal("skillleveling")
                                                .requires(source -> source.hasPermissionLevel(2))
                                                // GET: View current skill level
                                                .then(CommandManager.literal("get")
                                                                .then(CommandManager
                                                                                .argument("player", EntityArgumentType
                                                                                                .player())
                                                                                .then(CommandManager.argument(
                                                                                                "category",
                                                                                                net.puffish.skillsmod.commands.arguments.CategoryArgumentType
                                                                                                                .category())
                                                                                                .then(CommandManager
                                                                                                                .argument("skill",
                                                                                                                                net.puffish.skillsmod.commands.arguments.SkillArgumentType
                                                                                                                                                .skillFromCategory(
                                                                                                                                                                "category"))
                                                                                                                .executes(SkillLevelingCommand::getSkillLevel)))))
                                                // SET: Set skill level directly (adjusts points accordingly)
                                                .then(CommandManager.literal("set")
                                                                .then(CommandManager
                                                                                .argument("player", EntityArgumentType
                                                                                                .player())
                                                                                .then(CommandManager.argument(
                                                                                                "category",
                                                                                                net.puffish.skillsmod.commands.arguments.CategoryArgumentType
                                                                                                                .category())
                                                                                                .then(CommandManager
                                                                                                                .argument("skill",
                                                                                                                                net.puffish.skillsmod.commands.arguments.SkillArgumentType
                                                                                                                                                .skillFromCategory(
                                                                                                                                                                "category"))
                                                                                                                .then(CommandManager
                                                                                                                                .argument("level",
                                                                                                                                                IntegerArgumentType
                                                                                                                                                                .integer(0))
                                                                                                                                .executes(SkillLevelingCommand::setSkillLevel))))))
                                                // REFUND: Reduce skill level and refund points
                                                .then(CommandManager.literal("refund")
                                                                .then(CommandManager
                                                                                .argument("player", EntityArgumentType
                                                                                                .player())
                                                                                .then(CommandManager.argument(
                                                                                                "category",
                                                                                                net.puffish.skillsmod.commands.arguments.CategoryArgumentType
                                                                                                                .category())
                                                                                                .then(CommandManager
                                                                                                                .argument("skill",
                                                                                                                                net.puffish.skillsmod.commands.arguments.SkillArgumentType
                                                                                                                                                .skillFromCategory(
                                                                                                                                                                "category"))
                                                                                                                // refund
                                                                                                                // <player>
                                                                                                                // <category>
                                                                                                                // <skill>
                                                                                                                // -
                                                                                                                // refunds
                                                                                                                // 1
                                                                                                                // level
                                                                                                                .executes(ctx -> refundLevels(
                                                                                                                                ctx,
                                                                                                                                1))
                                                                                                                // refund
                                                                                                                // <player>
                                                                                                                // <category>
                                                                                                                // <skill>
                                                                                                                // <amount>
                                                                                                                .then(CommandManager
                                                                                                                                .argument("amount",
                                                                                                                                                IntegerArgumentType
                                                                                                                                                                .integer(1))
                                                                                                                                .suggests((ctx, builder) -> {
                                                                                                                                        try {
                                                                                                                                                var player = EntityArgumentType
                                                                                                                                                                .getPlayer(ctx, "player");
                                                                                                                                                var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType
                                                                                                                                                                .getCategory(ctx,
                                                                                                                                                                                "category");
                                                                                                                                                var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType
                                                                                                                                                                .getSkillFromCategory(
                                                                                                                                                                                ctx,
                                                                                                                                                                                "skill",
                                                                                                                                                                                category);
                                                                                                                                                int currentLevel = getLevel(
                                                                                                                                                                player,
                                                                                                                                                                category.getId(),
                                                                                                                                                                skill.getId());
                                                                                                                                                for (int i = 1; i <= currentLevel; i++) {
                                                                                                                                                        builder.suggest(String
                                                                                                                                                                        .valueOf(i));
                                                                                                                                                }
                                                                                                                                        } catch (Exception e) {
                                                                                                                                        }
                                                                                                                                        return builder.buildFuture();
                                                                                                                                })
                                                                                                                                .executes(ctx -> refundLevels(
                                                                                                                                                ctx,
                                                                                                                                                IntegerArgumentType
                                                                                                                                                                .getInteger(ctx, "amount"))))
                                                                                                                // refund
                                                                                                                // <player>
                                                                                                                // <category>
                                                                                                                // <skill>
                                                                                                                // all
                                                                                                                .then(CommandManager
                                                                                                                                .literal("all")
                                                                                                                                .executes(SkillLevelingCommand::refundAllLevels))))))
                                                // INFO: Show detailed skill information
                                                .then(CommandManager.literal("info")
                                                                .then(CommandManager
                                                                                .argument("player", EntityArgumentType
                                                                                                .player())
                                                                                .then(CommandManager.argument(
                                                                                                "category",
                                                                                                net.puffish.skillsmod.commands.arguments.CategoryArgumentType
                                                                                                                .category())
                                                                                                .then(CommandManager
                                                                                                                .argument("skill",
                                                                                                                                net.puffish.skillsmod.commands.arguments.SkillArgumentType
                                                                                                                                                .skillFromCategory(
                                                                                                                                                                "category"))
                                                                                                                .executes(SkillLevelingCommand::showSkillInfo)))))
                                                // LIST: List all skills for a player
                                                .then(CommandManager.literal("list")
                                                                .then(CommandManager
                                                                                .argument("player", EntityArgumentType
                                                                                                .player())
                                                                                .executes(SkillLevelingCommand::listPlayerSkills)))
                                                // PREREQUISITES: Show skill prerequisites
                                                .then(CommandManager.literal("prerequisites")
                                                                .then(CommandManager.argument("category",
                                                                                net.puffish.skillsmod.commands.arguments.CategoryArgumentType
                                                                                                .category())
                                                                                .then(CommandManager.argument("skill",
                                                                                                net.puffish.skillsmod.commands.arguments.SkillArgumentType
                                                                                                                .skillFromCategory(
                                                                                                                                "category"))
                                                                                                .executes(SkillLevelingCommand::showPrerequisites))))
                                                // CATEGORYLEVEL: Set player's category level (calculates XP needed)
                                                .then(CommandManager.literal("categorylevel")
                                                                .then(CommandManager
                                                                                .argument("player", EntityArgumentType
                                                                                                .player())
                                                                                .then(CommandManager.argument(
                                                                                                "category",
                                                                                                net.puffish.skillsmod.commands.arguments.CategoryArgumentType
                                                                                                                .category())
                                                                                                .then(CommandManager
                                                                                                                .argument("level",
                                                                                                                                IntegerArgumentType
                                                                                                                                                .integer(0))
                                                                                                                .executes(SkillLevelingCommand::setCategoryLevel))))));
        }

        /**
         * Get the current level of a skill for a player
         */
        private static int getSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                var source = context.getSource();
                var player = EntityArgumentType.getPlayer(context, "player");
                var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context,
                                "category");
                var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context,
                                "skill", category);

                int currentLevel = getLevel(player, category.getId(), skill.getId());
                int maxLevel = getMaxLevel(category.getId(), skill.getId());
                int points = net.bluelotuscoding.skillleveling.points.SkillPointManager.getCurrentPoints(player,
                                category.getId());

                String skillName = skill.getId().replace("_", " ");
                source.sendMessage(Text.literal(String.format(
                                "§6%s §7- Level §e%d§7/§e%d §7(§a%d§7 points available)",
                                skillName, currentLevel, maxLevel, points)));

                return currentLevel;
        }

        /**
         * Set the level of a skill for a player
         * This directly sets the level WITHOUT adjusting points (pure admin override)
         * Use the /skills points command to adjust points separately if needed
         */
        private static int setSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                var source = context.getSource();
                var player = EntityArgumentType.getPlayer(context, "player");
                var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context,
                                "category");
                var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context,
                                "skill", category);
                var targetLevel = IntegerArgumentType.getInteger(context, "level");

                int maxLevel = getMaxLevel(category.getId(), skill.getId());

                if (targetLevel < 0 || targetLevel > maxLevel) {
                        source.sendError(Text.literal(String.format(
                                        "§cLevel must be between 0 and %d", maxLevel)));
                        return 0;
                }

                // Set the level directly via CategoryData (NO point adjustment)
                boolean success = setLevelDirect(player, category.getId(), skill.getId(), targetLevel);

                if (success) {
                        // Sync to client
                        var addon = SkillLevelingMod.getInstance();
                        if (addon != null) {
                                addon.getSkillLevelingManager().syncSkillLevelToClient(player, category.getId(),
                                                skill.getId(), targetLevel, maxLevel);
                        }

                        String skillName = skill.getId().replace("_", " ");
                        source.sendMessage(Text.literal(String.format(
                                        "§aSet §6%s §ato level §e%d§7/§e%d§a for §6%s §7(points unchanged)",
                                        skillName, targetLevel, maxLevel, player.getName().getString())));
                        return 1;
                } else {
                        source.sendError(Text.literal("§cFailed to set skill level"));
                        return 0;
                }
        }

        /**
         * Refund a specific number of levels
         */
        private static int refundLevels(CommandContext<ServerCommandSource> context, int amount)
                        throws CommandSyntaxException {
                var source = context.getSource();
                var player = EntityArgumentType.getPlayer(context, "player");
                var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context,
                                "category");
                var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context,
                                "skill", category);

                int currentLevel = getLevel(player, category.getId(), skill.getId());

                if (currentLevel <= 0) {
                        source.sendError(Text.literal("§cSkill is already at level 0, nothing to refund"));
                        return 0;
                }

                int levelsToRefund = Math.min(amount, currentLevel);
                int newLevel = currentLevel - levelsToRefund;
                int maxLevel = getMaxLevel(category.getId(), skill.getId());

                // Set the correct level
                boolean success;
                if (newLevel <= 0) {
                        // If going to level 0, use the base mod's lock mechanism to ensure proper state
                        var categoryApi = net.puffish.skillsmod.api.SkillsAPI.getCategory(category.getId());
                        if (categoryApi.isPresent()) {
                                var skillApi = categoryApi.get().getSkill(skill.getId());
                                if (skillApi.isPresent()) {
                                        skillApi.get().lock(player);
                                        success = true;
                                } else {
                                        success = setLevelDirect(player, category.getId(), skill.getId(), 0);
                                }
                        } else {
                                success = setLevelDirect(player, category.getId(), skill.getId(), 0);
                        }
                } else {
                        success = setLevelDirect(player, category.getId(), skill.getId(), newLevel);
                }

                if (success) {
                        // TRIGGER POINT SYNC
                        // We do this by calling addPoints with 0 - it's a public API that triggers sync
                        var categoryApi = net.puffish.skillsmod.api.SkillsAPI.getCategory(category.getId());
                        categoryApi.ifPresent(api -> api.addPoints(player,
                                        net.puffish.skillsmod.util.PointSources.COMMANDS, 0));

                        // Sync our addon's level display
                        var addon = SkillLevelingMod.getInstance();
                        if (addon != null) {
                                addon.getSkillLevelingManager().syncSkillLevelToClient(player, category.getId(),
                                                skill.getId(), newLevel, maxLevel);
                        }

                        String skillName = skill.getId().replace("_", " ");
                        source.sendMessage(Text.literal(String.format(
                                        "§aRefunded §6%d §alevels of §6%s §a(now level §e%d§a)",
                                        levelsToRefund, skillName, newLevel)));
                        return 1;
                } else {
                        source.sendError(Text.literal("§cFailed to refund skill levels"));
                        return 0;
                }
        }

        /**
         * Refund all levels of a skill
         */
        private static int refundAllLevels(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                var source = context.getSource();
                var player = EntityArgumentType.getPlayer(context, "player");
                var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context,
                                "category");
                var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context,
                                "skill", category);

                int currentLevel = getLevel(player, category.getId(), skill.getId());

                if (currentLevel <= 0) {
                        source.sendError(Text.literal("§cSkill is already at level 0, nothing to refund"));
                        return 0;
                }

                return refundLevels(context, currentLevel);
        }

        // ============== HELPER METHODS ==============

        /**
         * Get skill level directly from CategoryData extension
         */
        private static int getLevel(net.minecraft.server.network.ServerPlayerEntity player,
                        net.minecraft.util.Identifier categoryId, String skillId) {
                try {
                        var skillsMod = SkillsMod.getInstance();
                        var getPlayerDataMethod = SkillsMod.class.getDeclaredMethod("getPlayerData",
                                        net.minecraft.server.network.ServerPlayerEntity.class);
                        getPlayerDataMethod.setAccessible(true);
                        var playerData = (PlayerData) getPlayerDataMethod.invoke(skillsMod, player);

                        if (playerData == null) {
                                return 0;
                        }

                        var getCategoryMethod = SkillsMod.class.getDeclaredMethod("getCategory",
                                        net.minecraft.util.Identifier.class);
                        getCategoryMethod.setAccessible(true);
                        var categoryConfigOpt = (java.util.Optional<?>) getCategoryMethod.invoke(skillsMod, categoryId);

                        if (categoryConfigOpt.isEmpty()) {
                                return 0;
                        }

                        var categoryConfig = (net.puffish.skillsmod.config.CategoryConfig) categoryConfigOpt.get();
                        var getOrCreateMethod = PlayerData.class.getDeclaredMethod("getOrCreateCategoryData",
                                        net.puffish.skillsmod.config.CategoryConfig.class);
                        getOrCreateMethod.setAccessible(true);
                        var categoryData = (CategoryData) getOrCreateMethod.invoke(playerData, categoryConfig);

                        if (categoryData instanceof CategoryDataExtension ext) {
                                return ext.addon$getSkillLevel(skillId);
                        }
                } catch (Exception e) {
                        // Ignore
                }
                return 0;
        }

        /**
         * Set skill level directly via CategoryData extension
         */
        private static boolean setLevelDirect(net.minecraft.server.network.ServerPlayerEntity player,
                        net.minecraft.util.Identifier categoryId, String skillId, int level) {
                try {
                        var skillsMod = SkillsMod.getInstance();
                        var getPlayerDataMethod = SkillsMod.class.getDeclaredMethod("getPlayerData",
                                        net.minecraft.server.network.ServerPlayerEntity.class);
                        getPlayerDataMethod.setAccessible(true);
                        var playerData = (PlayerData) getPlayerDataMethod.invoke(skillsMod, player);

                        if (playerData == null) {
                                return false;
                        }

                        var getCategoryMethod = SkillsMod.class.getDeclaredMethod("getCategory",
                                        net.minecraft.util.Identifier.class);
                        getCategoryMethod.setAccessible(true);
                        var categoryConfigOpt = (java.util.Optional<?>) getCategoryMethod.invoke(skillsMod, categoryId);

                        if (categoryConfigOpt.isEmpty()) {
                                return false;
                        }

                        var categoryConfig = (net.puffish.skillsmod.config.CategoryConfig) categoryConfigOpt.get();
                        var getOrCreateMethod = PlayerData.class.getDeclaredMethod("getOrCreateCategoryData",
                                        net.puffish.skillsmod.config.CategoryConfig.class);
                        getOrCreateMethod.setAccessible(true);
                        var categoryData = (CategoryData) getOrCreateMethod.invoke(playerData, categoryConfig);

                        if (categoryData instanceof CategoryDataExtension ext) {
                                ext.addon$setSkillLevel(skillId, level);
                                return true;
                        }
                } catch (Exception e) {
                        SkillLevelingMod.getInstance().getLogger().error("Failed to set level: " + e.getMessage());
                }
                return false;
        }

        /**
         * Get max level for a skill
         */
        private static int getMaxLevel(net.minecraft.util.Identifier categoryId, String skillId) {
                var addon = SkillLevelingMod.getInstance();
                if (addon != null) {
                        return addon.getSkillLevelingManager().getMaxLevel(categoryId, skillId);
                }
                return 1;
        }

        /**
         * Get points per level for a skill
         */
        private static int getPointsPerLevel(net.minecraft.util.Identifier categoryId, String skillId) {
                var addon = SkillLevelingMod.getInstance();
                if (addon != null) {
                        var reward = addon.getSkillLevelingManager().getPerLevelRewardsReward(categoryId, skillId);
                        if (reward.isPresent()) {
                                return reward.get().getPointsPerLevel();
                        }
                }
                return 1; // Default fallback
        }

        /**
         * Show detailed information about a skill
         */
        private static int showSkillInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                var source = context.getSource();
                var player = EntityArgumentType.getPlayer(context, "player");
                var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context,
                                "category");
                var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context,
                                "skill", category);

                int currentLevel = getLevel(player, category.getId(), skill.getId());
                int maxLevel = getMaxLevel(category.getId(), skill.getId());
                int pointsPerLevel = getPointsPerLevel(category.getId(), skill.getId());
                int points = net.bluelotuscoding.skillleveling.points.SkillPointManager.getCurrentPoints(player,
                                category.getId());

                String skillName = skill.getId().replace("_", " ");

                source.sendMessage(Text.literal("§6═══ Skill Information ═══"));
                source.sendMessage(Text.literal(String.format("§aPlayer: §e%s", player.getName().getString())));
                source.sendMessage(
                                Text.literal(String.format("§aSkill: §e%s §7(§e%s§7)", skillName, category.getId())));
                source.sendMessage(Text.literal(String.format("§aLevel: §6%d§7/§6%d", currentLevel, maxLevel)));
                source.sendMessage(Text.literal(String.format("§aPoints Per Level: §6%d", pointsPerLevel)));
                source.sendMessage(Text.literal(String.format("§aAvailable Points: §6%d", points)));

                if (currentLevel < maxLevel) {
                        int cost = pointsPerLevel;
                        boolean canAfford = points >= cost;
                        String affordMsg = canAfford ? "§a✓" : "§c✗";
                        source.sendMessage(
                                        Text.literal(String.format("§aNext Level Cost: §6%d §7(%s)", cost, affordMsg)));
                } else {
                        source.sendMessage(Text.literal("§dMAX LEVEL REACHED"));
                }

                return 1;
        }

        /**
         * List all skills with levels for a player
         */
        private static int listPlayerSkills(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                var source = context.getSource();
                var player = EntityArgumentType.getPlayer(context, "player");

                source.sendMessage(Text
                                .literal(String.format("§6═══ Skills for §e%s §6═══", player.getName().getString())));

                // Iterate through all categories and skills
                net.puffish.skillsmod.api.SkillsAPI.streamCategories().forEach(category -> {
                        category.streamSkills().forEach(skill -> {
                                int level = getLevel(player, category.getId(), skill.getId());
                                int maxLevel = getMaxLevel(category.getId(), skill.getId());

                                if (level > 0) {
                                        source.sendMessage(Text.literal(String.format(
                                                        "§a%s:%s §7- Level §6%d§7/§6%d",
                                                        category.getId().getPath(),
                                                        skill.getId(),
                                                        level,
                                                        maxLevel)));
                                }
                        });
                });

                return 1;
        }

        /**
         * Show prerequisites for a skill
         */
        private static int showPrerequisites(CommandContext<ServerCommandSource> context)
                        throws CommandSyntaxException {
                var source = context.getSource();
                var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context,
                                "category");
                var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context,
                                "skill", category);

                var addon = SkillLevelingMod.getInstance();
                var manager = addon.getSkillLevelingManager();

                var prerequisites = manager.getPrerequisiteInfo(category.getId(), skill.getId());

                source.sendMessage(Text.literal(String.format("§6═══ Prerequisites for §e%s §6═══", skill.getId())));
                source.sendMessage(Text.literal(String.format("§aCategory: §e%s", category.getId())));

                if (prerequisites.isEmpty()) {
                        source.sendMessage(Text.literal("§7No prerequisites required"));
                } else {
                        source.sendMessage(Text.literal("§aRequired Skills:"));
                        for (var prerequisite : prerequisites) {
                                source.sendMessage(Text.literal("§7• " + prerequisite));
                        }
                }

                return 1;
        }

        /**
         * Set a player's category level by calculating and setting the required XP
         */
        private static int setCategoryLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                var source = context.getSource();
                var player = EntityArgumentType.getPlayer(context, "player");
                var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context,
                                "category");
                var targetLevel = IntegerArgumentType.getInteger(context, "level");

                try {
                        // Access SkillsMod to get experience config and set experience
                        var skillsMod = net.puffish.skillsmod.SkillsMod.getInstance();

                        // Get the CategoryConfig using reflection
                        var getCategoryMethod = net.puffish.skillsmod.SkillsMod.class.getDeclaredMethod("getCategory",
                                        net.minecraft.util.Identifier.class);
                        getCategoryMethod.setAccessible(true);
                        var categoryConfigOpt = (java.util.Optional<?>) getCategoryMethod.invoke(skillsMod,
                                        category.getId());

                        if (categoryConfigOpt.isEmpty()) {
                                source.sendError(Text.literal("§cCategory not found"));
                                return 0;
                        }

                        var categoryConfig = (net.puffish.skillsmod.config.CategoryConfig) categoryConfigOpt.get();
                        var experienceOpt = categoryConfig.experience();

                        if (experienceOpt.isEmpty()) {
                                source.sendError(Text.literal("§cCategory does not have experience configured"));
                                return 0;
                        }

                        var experience = experienceOpt.get();
                        var curve = experience.curve();

                        // Check level limit
                        int levelLimit = curve.getLevelLimit();
                        if (targetLevel < 0 || targetLevel > levelLimit) {
                                source.sendError(Text.literal(
                                                String.format("§cLevel must be between 0 and %d", levelLimit)));
                                return 0;
                        }

                        // Calculate XP needed for target level
                        int requiredXP = 0;
                        if (targetLevel > 0) {
                                requiredXP = curve.getRequiredTotal(targetLevel - 1);
                        }

                        // Set the experience using SkillsMod.setExperience
                        skillsMod.setExperience(player, category.getId(), requiredXP);

                        source.sendMessage(Text.literal(String.format(
                                        "§aSet §6%s §acategory level to §e%d §afor §6%s §7(XP: %d)",
                                        category.getId().getPath(), targetLevel, player.getName().getString(),
                                        requiredXP)));
                        return 1;

                } catch (Exception e) {
                        source.sendError(Text.literal("§cFailed to set category level: " + e.getMessage()));
                        SkillLevelingMod.getInstance().getLogger().error("setCategoryLevel error: " + e);
                        return 0;
                }
        }
}
