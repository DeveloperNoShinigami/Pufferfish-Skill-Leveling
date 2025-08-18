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
        int level = addon.getSkillLevel(player, category.getId(), skill.getId());
        
        source.sendMessage(Text.literal(String.format(
                "§6%s §ahas §e%s §ain §e%s §aat level §6%d",
                player.getName().getString(),
                skill.getId(),
                category.getId(),
                level
        )));
        
        return level;
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
            source.sendMessage(Text.literal(String.format(
                    "§aSet §e%s §ain §e%s §afor §6%s §ato level §6%d",
                    skill.getId(),
                    category.getId(),
                    player.getName().getString(),
                    level
            )));
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
        boolean success = addon.advanceSkillLevel(player, category.getId(), skill.getId());
        
        if (success) {
            int newLevel = addon.getSkillLevel(player, category.getId(), skill.getId());
            source.sendMessage(Text.literal(String.format(
                    "§aAdvanced §e%s §ain §e%s §afor §6%s §ato level §6%d",
                    skill.getId(),
                    category.getId(),
                    player.getName().getString(),
                    newLevel
            )));
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
        boolean success = addon.refundSkillLevel(player, category.getId(), skill.getId());
        
        if (success) {
            int newLevel = addon.getSkillLevel(player, category.getId(), skill.getId());
            source.sendMessage(Text.literal(String.format(
                    "§aRefunded 1 level of §e%s §ain §e%s §afor §6%s §a(now level §6%d§a)",
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
}
