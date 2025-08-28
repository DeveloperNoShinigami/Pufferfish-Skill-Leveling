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
import net.bluelotuscoding.skillleveling.client.SkillLevelingClient;

/**
 * Commands for managing skill levels in the Pufferfish Skill Leveling addon
 */
public class SkillLevelingCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("skillleveling")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("get")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("category", net.puffish.skillsmod.commands.arguments.CategoryArgumentType.category())
                            .then(CommandManager.argument("skill", net.puffish.skillsmod.commands.arguments.SkillArgumentType.skillFromCategory("category"))
                                .executes(SkillLevelingCommand::getSkillLevel)
                            )
                        )
                    )
                )
                .then(CommandManager.literal("set")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("category", net.puffish.skillsmod.commands.arguments.CategoryArgumentType.category())
                            .then(CommandManager.argument("skill", net.puffish.skillsmod.commands.arguments.SkillArgumentType.skillFromCategory("category"))
                                .then(CommandManager.argument("level", IntegerArgumentType.integer(1))
                                    .executes(SkillLevelingCommand::setSkillLevel)
                                )
                            )
                        )
                    )
                )
                .then(CommandManager.literal("advance")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("category", net.puffish.skillsmod.commands.arguments.CategoryArgumentType.category())
                            .then(CommandManager.argument("skill", net.puffish.skillsmod.commands.arguments.SkillArgumentType.skillFromCategory("category"))
                                .executes(SkillLevelingCommand::advanceSkillLevel)
                            )
                        )
                    )
                )
                .then(CommandManager.literal("refund")
                    .then(CommandManager.literal("one")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("category", net.puffish.skillsmod.commands.arguments.CategoryArgumentType.category())
                                .then(CommandManager.argument("skill", net.puffish.skillsmod.commands.arguments.SkillArgumentType.skillFromCategory("category"))
                                    .executes(SkillLevelingCommand::refundSkillLevel)
                                )
                            )
                        )
                    )
                    .then(CommandManager.literal("multiple")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("category", net.puffish.skillsmod.commands.arguments.CategoryArgumentType.category())
                                .then(CommandManager.argument("skill", net.puffish.skillsmod.commands.arguments.SkillArgumentType.skillFromCategory("category"))
                                    .then(CommandManager.argument("levels", IntegerArgumentType.integer(1))
                                        .executes(SkillLevelingCommand::refundMultipleSkillLevels)
                                    )
                                )
                            )
                        )
                    )
                    .then(CommandManager.literal("all")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("category", net.puffish.skillsmod.commands.arguments.CategoryArgumentType.category())
                                .then(CommandManager.argument("skill", net.puffish.skillsmod.commands.arguments.SkillArgumentType.skillFromCategory("category"))
                                    .executes(SkillLevelingCommand::refundAllSkillLevels)
                                )
                            )
                        )
                    )
                )
                .then(CommandManager.literal("prerequisites")
                    .then(CommandManager.argument("category", net.puffish.skillsmod.commands.arguments.CategoryArgumentType.category())
                        .then(CommandManager.argument("skill", net.puffish.skillsmod.commands.arguments.SkillArgumentType.skillFromCategory("category"))
                            .executes(SkillLevelingCommand::showPrerequisites)
                        )
                    )
                )
                .then(CommandManager.literal("info")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("category", net.puffish.skillsmod.commands.arguments.CategoryArgumentType.category())
                            .then(CommandManager.argument("skill", net.puffish.skillsmod.commands.arguments.SkillArgumentType.skillFromCategory("category"))
                                .executes(SkillLevelingCommand::showSkillInfo)
                            )
                        )
                    )
                )
                .then(CommandManager.literal("list")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(SkillLevelingCommand::listPlayerSkills)
                    )
                )
                .then(CommandManager.literal("max")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("category", net.puffish.skillsmod.commands.arguments.CategoryArgumentType.category())
                            .then(CommandManager.argument("skill", net.puffish.skillsmod.commands.arguments.SkillArgumentType.skillFromCategory("category"))
                                .executes(SkillLevelingCommand::maxSkillLevel)
                            )
                        )
                    )
                )
        );
    }

    /**
     * Get the current level of a skill for a player
     */
    private static int getSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = EntityArgumentType.getPlayer(context, "player");
        var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context, "category");
        var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context, "skill", category);
        
        var addon = SkillLevelingMod.getInstance();
        var manager = addon.getSkillLevelingManager();
        
        int currentLevel = manager.getSkillLevel(player, category.getId(), skill.getId());
        int maxLevel = manager.getMaxLevel(category.getId(), skill.getId());
        
        // ENHANCED DISPLAY: Use client formatting for better visual presentation
        String skillName = skill.getId().replace("_", " ");
        String levelDisplay = SkillLevelingClient.formatSkillLevel(skillName, currentLevel, maxLevel);
        String progressBar = SkillLevelingClient.createProgressBar(currentLevel, maxLevel, 10);
        
        source.sendMessage(Text.literal("§6═══ Skill Information ═══"));
        source.sendMessage(Text.literal(String.format("§aPlayer: §e%s", player.getName().getString())));
        source.sendMessage(Text.literal(levelDisplay));
        source.sendMessage(Text.literal(progressBar));
        
        return currentLevel;
    }
    
    /**
     * Set the level of a skill for a player
     */
    private static int setSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = EntityArgumentType.getPlayer(context, "player");
        var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context, "category");
        var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context, "skill", category);
        var level = IntegerArgumentType.getInteger(context, "level");
        
        var addon = SkillLevelingMod.getInstance();
        boolean success = addon.setSkillLevel(player, category.getId(), skill.getId(), level);
        
        if (success) {
            var manager = addon.getSkillLevelingManager();
            int maxLevel = manager.getMaxLevel(category.getId(), skill.getId());
            
            // ENHANCED DISPLAY: Better visual feedback with client formatting
            String skillName = skill.getId().replace("_", " ");
            String levelDisplay = SkillLevelingClient.formatSkillLevel(skillName, level, maxLevel);
            String progressBar = SkillLevelingClient.createProgressBar(level, maxLevel, 10);
            
            source.sendMessage(Text.literal("§6═══ Skill Level Set ═══"));
            source.sendMessage(Text.literal(String.format("§aPlayer: §e%s", player.getName().getString())));
            source.sendMessage(Text.literal(levelDisplay));
            source.sendMessage(Text.literal(progressBar));
            
            return 1;
        } else {
            source.sendError(Text.literal(String.format(
                    "§cFailed to set §e%s §cin §e%s §cfor §6%s §cto level §6%d",
                    skill.getId(),
                    category.getId(),
                    player.getName().getString(),
                    level
            )));
            return 0;
        }
    }
    
    /**
     * Advance a skill to the next level for a player
     */
    private static int advanceSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = EntityArgumentType.getPlayer(context, "player");
        var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context, "category");
        var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context, "skill", category);
        
        var addon = SkillLevelingMod.getInstance();
        var manager = addon.getSkillLevelingManager();
        int oldLevel = manager.getSkillLevel(player, category.getId(), skill.getId());
        
        boolean success = addon.advanceSkillLevel(player, category.getId(), skill.getId());
        
        if (success) {
            int newLevel = manager.getSkillLevel(player, category.getId(), skill.getId());
            int maxLevel = manager.getMaxLevel(category.getId(), skill.getId());
            
            // ENHANCED DISPLAY: Use client formatting for level advancement
            String skillName = skill.getId().replace("_", " ");
            String levelUpMessage = SkillLevelingClient.createLevelUpMessage(skillName, oldLevel, newLevel);
            String progressBar = SkillLevelingClient.createProgressBar(newLevel, maxLevel, 10);
            
            source.sendMessage(Text.literal("§6═══ Skill Advanced ═══"));
            source.sendMessage(Text.literal(String.format("§aPlayer: §e%s", player.getName().getString())));
            source.sendMessage(Text.literal(levelUpMessage));
            source.sendMessage(Text.literal(progressBar));
            
            return 1;
        } else {
            source.sendError(Text.literal(String.format(
                    "§cCannot advance §e%s §cin §e%s §cfor §6%s §c(may be at maximum level)",
                    skill.getId(),
                    category.getId(),
                    player.getName().getString()
            )));
            return 0;
        }
    }
    
    /**
     * Refund one level of a skill
     */
    private static int refundSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = EntityArgumentType.getPlayer(context, "player");
        var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context, "category");
        var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context, "skill", category);
        
        var addon = SkillLevelingMod.getInstance();
        var manager = addon.getSkillLevelingManager();
        int oldLevel = manager.getSkillLevel(player, category.getId(), skill.getId());
        
        boolean success = addon.refundSkillLevel(player, category.getId(), skill.getId());
        
        if (success) {
            int newLevel = manager.getSkillLevel(player, category.getId(), skill.getId());
            int maxLevel = manager.getMaxLevel(category.getId(), skill.getId());
            
            // ENHANCED DISPLAY: Show refund with visual feedback
            String skillName = skill.getId().replace("_", " ");
            String refundMessage = String.format("§6%s §7level reduced from §c%d §7to §e%d §7(refunded)", 
                skillName, oldLevel, newLevel);
            String progressBar = SkillLevelingClient.createProgressBar(newLevel, maxLevel, 10);
            
            source.sendMessage(Text.literal("§6═══ Skill Refunded ═══"));
            source.sendMessage(Text.literal(String.format("§aPlayer: §e%s", player.getName().getString())));
            source.sendMessage(Text.literal(refundMessage));
            source.sendMessage(Text.literal(progressBar));
            
            return 1;
        } else {
            source.sendError(Text.literal(String.format(
                    "§cCannot refund §e%s §cin §e%s §cfor §6%s §c(may be at minimum level)",
                    skill.getId(),
                    category.getId(),
                    player.getName().getString()
            )));
            return 0;
        }
    }
    
    /**
     * Refund multiple levels of a skill
     */
    private static int refundMultipleSkillLevels(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = EntityArgumentType.getPlayer(context, "player");
        var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context, "category");
        var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context, "skill", category);
        var levels = IntegerArgumentType.getInteger(context, "levels");
        
        var addon = SkillLevelingMod.getInstance();
        int refunded = addon.refundSkillLevels(player, category.getId(), skill.getId(), levels);
        
        if (refunded > 0) {
            int newLevel = addon.getSkillLevel(player, category.getId(), skill.getId());
            source.sendMessage(Text.literal(String.format(
                    "§aRefunded §6%d §alevels of §e%s §ain §e%s §afor §6%s §a(now level §6%d§a)",
                    refunded,
                    skill.getId(),
                    category.getId(),
                    player.getName().getString(),
                    newLevel
            )));
            return 1;
        } else {
            source.sendError(Text.literal(String.format(
                    "§cCannot refund §e%s §cin §e%s §cfor §6%s §c(may be at minimum level)",
                    skill.getId(),
                    category.getId(),
                    player.getName().getString()
            )));
            return 0;
        }
    }
    
    /**
     * Refund all levels of a skill (except base level 1)
     */
    private static int refundAllSkillLevels(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = EntityArgumentType.getPlayer(context, "player");
        var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context, "category");
        var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context, "skill", category);
        
        var addon = SkillLevelingMod.getInstance();
        int refunded = addon.refundAllSkillLevels(player, category.getId(), skill.getId());
        
        if (refunded > 0) {
            source.sendMessage(Text.literal(String.format(
                    "§aRefunded all §6%d §alevels of §e%s §ain §e%s §afor §6%s §a(reset to level 1)",
                    refunded,
                    skill.getId(),
                    category.getId(),
                    player.getName().getString()
            )));
            return 1;
        } else {
            source.sendError(Text.literal(String.format(
                    "§cNo levels to refund for §e%s §cin §e%s §cfor §6%s §c(already at minimum level)",
                    skill.getId(),
                    category.getId(),
                    player.getName().getString()
            )));
            return 0;
        }
    }
    
    /**
     * Show detailed information about a skill's levels and progression
     */
    private static int showSkillInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = EntityArgumentType.getPlayer(context, "player");
        var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context, "category");
        var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context, "skill", category);
        
        var addon = SkillLevelingMod.getInstance();
        var manager = addon.getSkillLevelingManager();
        
        int currentLevel = manager.getSkillLevel(player, category.getId(), skill.getId());
        int maxLevel = manager.getMaxLevel(category.getId(), skill.getId());
        int pointsPerLevel = manager.getPointsForLevel(category.getId(), skill.getId(), currentLevel + 1);
        
        // Build comprehensive skill information
        source.sendMessage(Text.literal("§6═══ Skill Information ═══"));
        source.sendMessage(Text.literal(String.format("§aPlayer: §e%s", player.getName().getString())));
        source.sendMessage(Text.literal(String.format("§aSkill: §e%s §7(§e%s§7)", skill.getId(), category.getId())));
        source.sendMessage(Text.literal(String.format("§aLevel: §6%d§7/§6%d", currentLevel, maxLevel)));
        
        if (pointsPerLevel > 0) {
            int currentPoints = net.bluelotuscoding.skillleveling.points.SkillPointManager.getCurrentPoints(player, category.getId());
            source.sendMessage(Text.literal(String.format("§aNext Level Cost: §6%d §7points (§6%d §7available)", pointsPerLevel, currentPoints)));
        } else {
            source.sendMessage(Text.literal("§aNext Level Cost: §eFree"));
        }
        
        // Show current level description if available
        String description = manager.getDescriptionForLevel(category.getId(), skill.getId(), currentLevel);
        if (!description.isEmpty()) {
            source.sendMessage(Text.literal("§aLevel Description:"));
            source.sendMessage(Text.literal("§7" + description));
        }
        
        // Show prerequisites if any exist
        var prerequisites = manager.getPrerequisiteInfo(category.getId(), skill.getId());
        if (!prerequisites.isEmpty()) {
            source.sendMessage(Text.literal("§aPrerequisites:"));
            for (var prerequisite : prerequisites) {
                boolean met = manager.checkSkillPrerequisites(player, category.getId(), skill.getId());
                String status = met ? "§a✓" : "§c✗";
                source.sendMessage(Text.literal(String.format("  %s §7%s", status, prerequisite)));
            }
        }
        
        // Show scaling information if scaling is enabled
        var reward = manager.getPerLevelRewardsReward(category.getId(), skill.getId());
        if (reward.isPresent() && reward.get().getScalingFactor() != 1.0) {
            source.sendMessage(Text.literal(String.format("§aScaling Factor: §6%.2f", reward.get().getScalingFactor())));
            source.sendMessage(Text.literal("§7(Point costs increase exponentially with level)"));
        }
        
        // Show next level preview if not at max
        if (currentLevel < maxLevel) {
            String nextDescription = manager.getDescriptionForLevel(category.getId(), skill.getId(), currentLevel + 1);
            if (!nextDescription.isEmpty()) {
                source.sendMessage(Text.literal("§aNext Level Preview:"));
                source.sendMessage(Text.literal("§8" + nextDescription));
            }
        }
        
        return 1;
    }
    
    /**
     * List all skills with levels for a player
     */
    private static int listPlayerSkills(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = EntityArgumentType.getPlayer(context, "player");
        
        var addon = SkillLevelingMod.getInstance();
        var manager = addon.getSkillLevelingManager();
        
        source.sendMessage(Text.literal(String.format("§6═══ Skills for §e%s §6═══", player.getName().getString())));
        
        int skillCount = 0;
        
        // Iterate through all categories and skills
        net.puffish.skillsmod.api.SkillsAPI.streamCategories().forEach(category -> {
            category.streamSkills().forEach(skill -> {
                // Only show skills that have level data (are leveled)
                if (manager.hasSkillData(player, category.getId(), skill.getId())) {
                    int level = manager.getSkillLevel(player, category.getId(), skill.getId());
                    int maxLevel = manager.getMaxLevel(category.getId(), skill.getId());
                    
                    source.sendMessage(Text.literal(String.format(
                        "§a%s:%s §7- Level §6%d§7/§6%d",
                        category.getId().getPath(),
                        skill.getId(),
                        level,
                        maxLevel
                    )));
                }
            });
        });
        
        if (skillCount == 0) {
            source.sendMessage(Text.literal("§7No leveled skills found for this player."));
        }
        
        return 1;
    }
    
    /**
     * Maximize a skill to its maximum level
     */
    private static int maxSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = EntityArgumentType.getPlayer(context, "player");
        var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context, "category");
        var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context, "skill", category);
        
        var addon = SkillLevelingMod.getInstance();
        var manager = addon.getSkillLevelingManager();
        
        int maxLevel = manager.getMaxLevel(category.getId(), skill.getId());
        boolean success = manager.setSkillLevel(player, category.getId(), skill.getId(), maxLevel);
        
        if (success) {
            source.sendMessage(Text.literal(String.format(
                    "§aMaximized §e%s §ain §e%s §afor §6%s §a(set to level §6%d§a)",
                    skill.getId(),
                    category.getId(),
                    player.getName().getString(),
                    maxLevel
            )));
            return 1;
        } else {
            source.sendError(Text.literal(String.format(
                    "§cFailed to maximize §e%s §cin §e%s §cfor §6%s",
                    skill.getId(),
                    category.getId(),
                    player.getName().getString()
            )));
            return 0;
        }
    }
    
    /**
     * Show prerequisites for a skill
     */
    private static int showPrerequisites(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var source = context.getSource();
        var category = net.puffish.skillsmod.commands.arguments.CategoryArgumentType.getCategory(context, "category");
        var skill = net.puffish.skillsmod.commands.arguments.SkillArgumentType.getSkillFromCategory(context, "skill", category);
        
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
            
            // Show reward configuration info
            var reward = manager.getPerLevelRewardsReward(category.getId(), skill.getId());
            if (reward.isPresent()) {
                var r = reward.get();
                source.sendMessage(Text.literal(""));
                source.sendMessage(Text.literal("§aReward Configuration:"));
                source.sendMessage(Text.literal(String.format("  §7Allow Partial Rewards: §e%s", r.allowsPartialRewards())));
                if (r.getScalingFactor() != 1.0) {
                    source.sendMessage(Text.literal(String.format("  §7Scaling Factor: §6%.2f", r.getScalingFactor())));
                }
            }
        }
        
        return 1;
    }
}
