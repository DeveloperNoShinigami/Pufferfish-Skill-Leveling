package net.bluelotuscoding.skillleveling.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.mixin_interface.CategoryDataExtension;
import net.minecraft.village.VillagerData;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.commands.arguments.CategoryArgumentType;
import net.puffish.skillsmod.commands.arguments.SkillArgumentType;
import net.puffish.skillsmod.server.data.PlayerData;
import net.puffish.skillsmod.server.data.CategoryData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.passive.VillagerEntity;

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
                var root = CommandManager.literal("skillleveling")
                                .requires(source -> source.hasPermissionLevel(2));

                // VILLAGER
                root.then(CommandManager.literal("villager")
                                .then(CommandManager.literal("setTier")
                                                .then(CommandManager.argument("tier", IntegerArgumentType.integer(1, 5))
                                                                .executes(SkillLevelingCommand::setVillagerTier)))
                                .then(CommandManager.literal("forceProfession")
                                                .executes(SkillLevelingCommand::forceVillagerProfession))
                                .then(CommandManager.literal("reset")
                                                .executes(SkillLevelingCommand::resetVillager)));

                // INTEGRATIONS
                root.then(CommandManager.literal("integrations")
                                .then(CommandManager.argument("modid", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                        builder.suggest("epicclassmod");
                                                        return builder.buildFuture();
                                                })
                                                .then(CommandManager.argument("action", StringArgumentType.word())
                                                                .suggests((ctx, builder) -> {
                                                                        String modid = StringArgumentType.getString(ctx,
                                                                                        "modid");
                                                                        if ("epicclassmod".equals(modid)) {
                                                                                builder.suggest("clear_given");
                                                                                builder.suggest("clear_stats");
                                                                                builder.suggest("set_sp");
                                                                        }
                                                                        return builder.buildFuture();
                                                                })
                                                                .executes(SkillLevelingCommand::handleIntegration)
                                                                .then(CommandManager
                                                                                .argument("amount", IntegerArgumentType
                                                                                                .integer())
                                                                                .executes(SkillLevelingCommand::handleIntegration)))));

                // ADMIN
                root.then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(4))
                                .then(CommandManager.literal("cleanup")
                                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                                                .executes(SkillLevelingCommand::cleanupPlayer)))
                                .then(CommandManager.literal("world_cleanup")
                                                .executes(SkillLevelingCommand::cleanupWorld)));

                // GET
                root.then(CommandManager.literal("get")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager
                                                                .argument("category", CategoryArgumentType.category())
                                                                .then(CommandManager.argument("skill",
                                                                                SkillArgumentType.skillFromCategory(
                                                                                                "category"))
                                                                                .executes(SkillLevelingCommand::getSkillLevel)))));

                // SET
                root.then(CommandManager.literal("set")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager
                                                                .argument("category", CategoryArgumentType.category())
                                                                .then(CommandManager.argument("skill",
                                                                                SkillArgumentType.skillFromCategory(
                                                                                                "category"))
                                                                                .then(CommandManager.argument("level",
                                                                                                IntegerArgumentType
                                                                                                                .integer(0))
                                                                                                .executes(SkillLevelingCommand::setSkillLevel))))));

                // REFUND
                root.then(CommandManager.literal("refund")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager
                                                                .argument("category", CategoryArgumentType.category())
                                                                .then(CommandManager.argument("skill",
                                                                                SkillArgumentType.skillFromCategory(
                                                                                                "category"))
                                                                                .executes(ctx -> refundLevels(ctx, 1))
                                                                                .then(CommandManager.argument("amount",
                                                                                                IntegerArgumentType
                                                                                                                .integer(1))
                                                                                                .suggests((ctx, builder) -> {
                                                                                                        try {
                                                                                                                var player = EntityArgumentType
                                                                                                                                .getPlayer(ctx, "player");
                                                                                                                Category category = CategoryArgumentType
                                                                                                                                .getCategory(ctx,
                                                                                                                                                "category");
                                                                                                                Skill skill = SkillArgumentType
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
                                                                                .then(CommandManager.literal("all")
                                                                                                .executes(SkillLevelingCommand::refundAllLevels))))));

                // INFO
                root.then(CommandManager.literal("info")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager
                                                                .argument("category", CategoryArgumentType.category())
                                                                .then(CommandManager.argument("skill",
                                                                                SkillArgumentType.skillFromCategory(
                                                                                                "category"))
                                                                                .executes(SkillLevelingCommand::showSkillInfo)))));

                // LIST
                root.then(CommandManager.literal("list")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(SkillLevelingCommand::listPlayerSkills)));

                // PREREQUISITES
                root.then(CommandManager.literal("prerequisites")
                                .then(CommandManager.argument("category", CategoryArgumentType.category())
                                                .then(CommandManager
                                                                .argument("skill",
                                                                                SkillArgumentType.skillFromCategory(
                                                                                                "category"))
                                                                .executes(SkillLevelingCommand::showPrerequisites))));

                // CATEGORYLEVEL
                root.then(CommandManager.literal("categorylevel")
                                .then(CommandManager.literal("sync")
                                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                                                .then(CommandManager.argument("category",
                                                                                CategoryArgumentType.category())
                                                                                .then(CommandManager.argument("skill",
                                                                                                SkillArgumentType
                                                                                                                .skillFromCategory(
                                                                                                                                "category"))
                                                                                                .executes(SkillLevelingCommand::forceSync)))))
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager
                                                                .argument("category", CategoryArgumentType.category())
                                                                .then(CommandManager
                                                                                .argument("level", IntegerArgumentType
                                                                                                .integer(0))
                                                                                .executes(SkillLevelingCommand::setCategoryLevel)))));

                // GIVE
                root.then(CommandManager.literal("give")
                                .then(CommandManager.literal("tome")
                                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                                                .then(CommandManager.argument("category",
                                                                                CategoryArgumentType.category())
                                                                                .then(CommandManager.argument("skill",
                                                                                                SkillArgumentType
                                                                                                                .skillFromCategory(
                                                                                                                                "category"))
                                                                                                .then(CommandManager
                                                                                                                .argument("loot_mode",
                                                                                                                                StringArgumentType
                                                                                                                                                .word())
                                                                                                                .suggests((ctx, builder) -> {
                                                                                                                        builder.suggest("both");
                                                                                                                        builder.suggest("tome_only");
                                                                                                                        builder.suggest("imbue_only");
                                                                                                                        return builder.buildFuture();
                                                                                                                })
                                                                                                                .then(CommandManager
                                                                                                                                .argument("level",
                                                                                                                                                IntegerArgumentType
                                                                                                                                                                .integer(1))
                                                                                                                                .executes(SkillLevelingCommand::giveSkillTome))
                                                                                                                .executes(ctx -> giveSkillTome(
                                                                                                                                ctx,
                                                                                                                                1))))))));

                // ADVANCECLASS
                root.then(CommandManager.literal("advanceclass")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(SkillLevelingCommand::advanceClass))
                                .executes(ctx -> {
                                        try {
                                                return advanceClass(ctx, ctx.getSource().getPlayerOrThrow());
                                        } catch (Exception e) {
                                                return 0;
                                        }
                                }));

                dispatcher.register(root);
        }

        private static int advanceClass(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                return advanceClass(context, EntityArgumentType.getPlayer(context, "player"));
        }

        private static int advanceClass(CommandContext<ServerCommandSource> context, ServerPlayerEntity player)
                        throws CommandSyntaxException {
                var source = context.getSource();
                var platform = net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getPlatform();

                // 1. Check player's current class using Platform abstraction
                String currentClassName = platform.getEpicClassName(player);

                if (currentClassName == null || currentClassName.isEmpty()
                                || currentClassName.equals("epic_classes:none")) {
                        source.sendMessage(Text.literal("§cYou do not have a base class selected."));
                        return 0;
                }

                // 2. Query getChildClasses
                java.util.List<net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef> children = net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager
                                .getChildClasses(currentClassName);

                if (children.isEmpty()) {
                        source.sendMessage(Text.literal("§cYour class has no further advancements available."));
                        return 0;
                }

                // 3. Validate Max Pufferfish Tier
                net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef def = net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager
                                .getClassDef(currentClassName);

                if (def == null) {
                        source.sendMessage(Text.literal("§cClass definition not found for: " + currentClassName));
                        return 0;
                }

                if (def.required_level > 0) {
                        int playerLevel = platform.getEpicClassPlayerLevel(player);
                        if (playerLevel < def.required_level) {
                                source.sendMessage(Text.literal(
                                                "§cYou must reach level §e" + def.required_level
                                                                + " §cbefore advancing. (Current level: §e"
                                                                + playerLevel + "§c)"));
                                return 0;
                        }
                }

                // 4. Send Packet via Platform to open UI
                platform.sendAdvanceClassScreen(player, currentClassName);
                source.sendMessage(Text.literal("§aOpening Class Advancement screen..."));
                return 1;
        }

        /**
         * Get the current level of a skill for a player
         */
        private static int getSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                var source = context.getSource();
                var player = EntityArgumentType.getPlayer(context, "player");
                Category category = CategoryArgumentType.getCategory(context, "category");
                Skill skill = SkillArgumentType.getSkillFromCategory(context, "skill", category);

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
                Category category = CategoryArgumentType.getCategory(context, "category");
                Skill skill = SkillArgumentType.getSkillFromCategory(context, "skill", category);
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
                                int totalLevel = addon.getSkillLevelingManager().getTotalSkillLevel(player,
                                                category.getId(),
                                                skill.getId());
                                addon.getSkillLevelingManager().syncSkillLevelToClient(player, category.getId(),
                                                skill.getId(), targetLevel, totalLevel, maxLevel);
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
                Category category = CategoryArgumentType.getCategory(context, "category");
                Skill skill = SkillArgumentType.getSkillFromCategory(context, "skill", category);

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
                                int totalLevel = addon.getSkillLevelingManager().getTotalSkillLevel(player,
                                                category.getId(),
                                                skill.getId());
                                addon.getSkillLevelingManager().syncSkillLevelToClient(player, category.getId(),
                                                skill.getId(), newLevel, totalLevel, maxLevel);
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
                Category category = CategoryArgumentType.getCategory(context, "category");
                Skill skill = SkillArgumentType.getSkillFromCategory(context, "skill", category);

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
         * Force-sync level and descriptions to a player for debugging.
         */
        private static int forceSync(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                var source = context.getSource();
                var player = EntityArgumentType.getPlayer(context, "player");
                Category category = CategoryArgumentType.getCategory(context, "category");
                Skill skill = SkillArgumentType.getSkillFromCategory(context, "skill", category);

                var addon = SkillLevelingMod.getInstance();
                if (addon == null) {
                        source.sendError(Text.literal("§cAddon not initialized"));
                        return 0;
                }

                var manager = addon.getSkillLevelingManager();

                int currentLevel = getLevel(player, category.getId(), skill.getId());
                int maxLevel = getMaxLevel(category.getId(), skill.getId());

                int totalLevel = manager.getTotalSkillLevel(player, category.getId(), skill.getId());
                // Send level update (base and total)
                manager.syncSkillLevelToClient(player, category.getId(), skill.getId(), currentLevel, totalLevel,
                                maxLevel);

                // Send descriptions if available
                var reward = manager.getPerLevelRewardsReward(category.getId(), skill.getId());
                if (reward.isPresent()) {
                        var plr = reward.get();
                        // Try to obtain definitionId via reflection; fallback to skill id
                        String definitionId = null;
                        try {
                                var method = skill.getClass().getMethod("definitionId");
                                definitionId = (String) method.invoke(skill);
                        } catch (Exception e) {
                                try {
                                        definitionId = skill.getId();
                                } catch (Exception ignored) {
                                }
                        }

                        if (definitionId != null) {
                                manager.syncDescriptionsToClient(player, definitionId, plr.getLevelDescriptions(),
                                                plr.getLevelExtraDescriptions(), plr.isMergeDescription(),
                                                plr.getMaxLevel());
                        }
                }

                source.sendMessage(Text.literal("§aForced sync sent"));
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
                Category category = CategoryArgumentType.getCategory(context, "category");
                Skill skill = SkillArgumentType.getSkillFromCategory(context, "skill", category);

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
                net.puffish.skillsmod.api.SkillsAPI.streamCategories()
                                .forEach((net.puffish.skillsmod.api.Category category) -> {
                                        category.streamSkills().forEach((net.puffish.skillsmod.api.Skill skill) -> {
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
                Category category = CategoryArgumentType.getCategory(context, "category");
                Skill skill = SkillArgumentType.getSkillFromCategory(context, "skill", category);

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
                Category category = CategoryArgumentType.getCategory(context, "category");
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
                        var experienceOpt = (java.util.Optional<?>) categoryConfig.getClass().getMethod("experience")
                                        .invoke(categoryConfig);

                        if (experienceOpt.isEmpty()) {
                                source.sendError(Text.literal("§cCategory does not have experience configured"));
                                return 0;
                        }

                        var experience = experienceOpt.get();
                        var curve = experience.getClass().getMethod("curve").invoke(experience);

                        // Check level limit
                        int levelLimit = (int) curve.getClass().getMethod("getLevelLimit").invoke(curve);
                        if (targetLevel < 0 || targetLevel > levelLimit) {
                                source.sendError(Text.literal(
                                                String.format("§cLevel must be between 0 and %d", levelLimit)));
                                return 0;
                        }

                        // Calculate XP needed for target level
                        int requiredXP = 0;
                        if (targetLevel > 0) {
                                requiredXP = (int) curve.getClass().getMethod("getRequiredTotal", int.class)
                                                .invoke(curve, targetLevel - 1);
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
                        return 0;
                }
        }

        private static int cleanupPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                var source = context.getSource();
                var player = EntityArgumentType.getPlayer(context, "player");

                source.sendMessage(Text.literal("§eStarting data cleanup for " + player.getName().getString() + "..."));

                // 1. Cleanup Pufferfish Data via Manager
                try {
                        var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
                        manager.clearAllData(player);
                } catch (Exception e) {
                        source.sendError(Text.literal("§cFailed to clear Pufferfish data: " + e.getMessage()));
                }

                // 2. Cleanup Cross-Mod Data via Platform
                try {
                        var platform = SkillLevelingMod.getInstance().getPlatform();
                        platform.cleanupPlayerData(player);
                } catch (Exception e) {
                        source.sendError(Text.literal("§cFailed to clear cross-mod data: " + e.getMessage()));
                }

                source.sendMessage(Text.literal("§aCleanup complete for " + player.getName().getString()
                                + ". Traces of skill/class data have been removed from this player."));
                return 1;
        }

        private static int cleanupWorld(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                var source = context.getSource();
                var level = source.getWorld();

                source.sendMessage(Text.literal("§eStarting world-wide data cleanup..."));

                // Cleanup World Data via Platform
                try {
                        var platform = SkillLevelingMod.getInstance().getPlatform();
                        platform.cleanupWorldData(level);
                } catch (Exception e) {
                        source.sendError(Text.literal("§cFailed to clear world data: " + e.getMessage()));
                        return 0;
                }

                source.sendMessage(Text.literal(
                                "§aWorld cleanup complete. All NPC spawn records and shared world data have been cleared."));
                return 1;
        }

        /**
         * Set the tier of a Skill Master villager being looked at
         */
        private static int setVillagerTier(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                var source = context.getSource();
                ServerPlayerEntity player = source.getPlayer();
                if (player == null) {
                        source.sendError(Text.literal("Command must be run by a player"));
                        return 0;
                }
                int tier = IntegerArgumentType.getInteger(context, "tier");

                double reach = 10.0;
                Vec3d start = player.getEyePos();
                Vec3d direction = player.getRotationVec(1.0F);
                Vec3d end = start.add(direction.x * reach, direction.y * reach, direction.z * reach);

                EntityHitResult hit = ProjectileUtil.raycast(
                                player, start, end,
                                player.getBoundingBox().stretch(direction.multiply(reach)).expand(1.0),
                                (entity) -> entity instanceof VillagerEntity v
                                                && v.getVillagerData()
                                                                .getProfession() == net.bluelotuscoding.skillleveling.registry.ModVillagers.SKILL_MASTER,
                                reach * reach);

                if (hit != null && hit.getEntity() instanceof VillagerEntity villager) {
                        VillagerData data = villager.getVillagerData();
                        villager.setVillagerData(data.withLevel(tier));
                        // Clear current trades so they refresh with new tier
                        villager.getOffers().clear();
                        source.sendMessage(Text.literal("§aSet Skill Master tier to §e" + tier));
                        return 1;
                } else {
                        source.sendError(Text
                                        .literal("§cYou must be looking at a Skill Master villager within 10 blocks!"));
                        return 0;
                }
        }

        /**
         * Force a villager to become a Skill Master
         */
        private static int forceVillagerProfession(CommandContext<ServerCommandSource> context)
                        throws CommandSyntaxException {
                var source = context.getSource();
                ServerPlayerEntity player = source.getPlayer();
                if (player == null) {
                        source.sendError(Text.literal("Command must be run by a player"));
                        return 0;
                }

                double reach = 10.0;
                Vec3d start = player.getEyePos();
                Vec3d direction = player.getRotationVec(1.0F);
                Vec3d end = start.add(direction.x * reach, direction.y * reach, direction.z * reach);

                EntityHitResult hit = ProjectileUtil.raycast(
                                player, start, end,
                                player.getBoundingBox().stretch(direction.multiply(reach)).expand(1.0),
                                (entity) -> entity instanceof VillagerEntity,
                                reach * reach);

                if (hit != null && hit.getEntity() instanceof VillagerEntity villager) {
                        villager.setVillagerData(villager.getVillagerData()
                                        .withProfession(net.bluelotuscoding.skillleveling.registry.ModVillagers.SKILL_MASTER));
                        source.sendMessage(Text.literal("§aForced villager to become a Skill Master!"));
                        return 1;
                } else {
                        source.sendError(Text.literal("§cYou must be looking at a villager within 10 blocks!"));
                        return 0;
                }
        }

        /**
         * Reset a villager's tier and experience
         */
        private static int resetVillager(CommandContext<ServerCommandSource> context)
                        throws CommandSyntaxException {
                var source = context.getSource();
                ServerPlayerEntity player = source.getPlayer();
                if (player == null) {
                        source.sendError(Text.literal("Command must be run by a player"));
                        return 0;
                }

                double reach = 10.0;
                Vec3d start = player.getEyePos();
                Vec3d direction = player.getRotationVec(1.0F);
                Vec3d end = start.add(direction.x * reach, direction.y * reach, direction.z * reach);

                EntityHitResult hit = ProjectileUtil.raycast(
                                player, start, end,
                                player.getBoundingBox().stretch(direction.multiply(reach)).expand(1.0),
                                (entity) -> entity instanceof VillagerEntity v
                                                && v.getVillagerData()
                                                                .getProfession() == net.bluelotuscoding.skillleveling.registry.ModVillagers.SKILL_MASTER,
                                reach * reach);

                if (hit != null && hit.getEntity() instanceof VillagerEntity villager) {
                        VillagerData data = villager.getVillagerData();
                        villager.setVillagerData(data.withLevel(1));
                        villager.setExperience(0);
                        villager.getOffers().clear();
                        source.sendMessage(Text.literal("§aReset Skill Master to Tier 1 with 0 experience."));
                        return 1;
                } else {
                        source.sendError(Text
                                        .literal("§cYou must be looking at a Skill Master villager within 10 blocks!"));
                        return 0;
                }
        }

        /**
         * Give a Skill Tome to a player
         */
        private static int giveSkillTome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                int level = IntegerArgumentType.getInteger(context, "level");
                return giveSkillTome(context, level);
        }

        private static int giveSkillTome(CommandContext<ServerCommandSource> context, int level)
                        throws CommandSyntaxException {
                var source = context.getSource();
                var player = EntityArgumentType.getPlayer(context, "player");
                Category category = CategoryArgumentType.getCategory(context, "category");
                Skill skill = SkillArgumentType.getSkillFromCategory(context, "skill", category);

                String lootMode = "both";
                try {
                        lootMode = StringArgumentType.getString(context, "loot_mode");
                } catch (Exception ignored) {
                }

                ItemStack tome = net.bluelotuscoding.skillleveling.item.SkillTomeItem.createSkillTome(
                                net.bluelotuscoding.skillleveling.registry.ModItems.SKILL_TOME,
                                category.getId().toString(),
                                skill.getId(),
                                lootMode,
                                level);

                if (player.getInventory().insertStack(tome)) {
                        source.sendMessage(Text.literal("§aGiven §e" + skill.getId() + " §aSkill Tome to §6"
                                        + player.getName().getString()));
                        return 1;
                } else {
                        source.sendError(Text.literal("§cInventory full for " + player.getName().getString()));
                        return 0;
                }
        }

        /**
         * Handle integration commands (e.g. clearing given tags for epicclassmod)
         */
        private static int handleIntegration(CommandContext<ServerCommandSource> context)
                        throws CommandSyntaxException {
                var source = context.getSource();
                String modid = StringArgumentType.getString(context, "modid");
                String action = StringArgumentType.getString(context, "action");

                if ("epicclassmod".equals(modid)) {
                        if ("clear_given".equals(action)) {
                                ServerPlayerEntity player = source.getPlayer();
                                if (player == null) {
                                        source.sendError(Text.literal(
                                                        "Must be run by a player to target self, or add player arg (TODO)"));
                                        return 0;
                                }

                                var data = net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance()
                                                .getPlatform().getPersistentData(player);
                                int cleared = 0;
                                var keys = new java.util.ArrayList<>(data.getKeys());
                                for (String key : keys) {
                                        if (key.startsWith("ecm_start_items_") || key.startsWith("ecm_items_received_")
                                                        || key.equals("ecm_class_chosen")
                                                        || key.equals("ecm_mana_applied_once")
                                                        || key.equals("ecm_applied_attributes")) {
                                                data.remove(key);
                                                cleared++;
                                        }
                                }

                                if (cleared > 0) {
                                        source.sendMessage(Text.literal("§aSuccessfully cleared §e" + cleared
                                                        + "§a integration tags for §6" + player.getName().getString()));
                                        return 1;
                                } else {
                                        source.sendError(
                                                        Text.literal("§cFailed to clear any tags or none were found."));
                                        return 0;
                                }
                        } else if ("clear_stats".equals(action)) {
                                ServerPlayerEntity player = source.getPlayer();
                                if (player == null) {
                                        source.sendError(Text.literal("Must be run by a player"));
                                        return 0;
                                }
                                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getPlatform()
                                                .resetEpicClassStats(player);
                                source.sendMessage(Text.literal("§aReset and refunded Epic Class stat points for §6"
                                                + player.getName().getString()));
                                return 1;
                        } else if ("set_sp".equals(action)) {
                                ServerPlayerEntity player = source.getPlayer();
                                if (player == null) {
                                        source.sendError(Text.literal("Must be run by a player"));
                                        return 0;
                                }
                                try {
                                        int amount = IntegerArgumentType.getInteger(context, "amount");
                                        net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getPlatform()
                                                        .setEpicClassStatPoints(player, amount);
                                        source.sendMessage(Text.literal("§aSet Epic Class stat points to §e" + amount
                                                        + " §afor §6" + player.getName().getString()));
                                        return 1;
                                } catch (Exception e) {
                                        source.sendError(Text.literal("§cUsage: ... set_sp <amount>"));
                                        return 0;
                                }
                        }
                }

                source.sendError(Text.literal("§cUnknown integration or action: " + modid + "/" + action));
                return 0;
        }

}
