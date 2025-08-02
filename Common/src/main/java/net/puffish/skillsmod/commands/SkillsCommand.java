package net.puffish.skillsmod.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.puffish.skillsmod.commands.arguments.CategoryArgumentType;
import net.puffish.skillsmod.commands.arguments.SkillArgumentType;
import net.puffish.skillsmod.util.CommandUtils;
import net.puffish.skillsmod.SkillsMod;

public class SkillsCommand {
	public static LiteralArgumentBuilder<ServerCommandSource> create() {
		return CommandManager.literal("skills")
				.requires(source -> source.hasPermissionLevel(2))
				.then(CommandManager.literal("unlock")
						.then(CommandManager.argument("players", EntityArgumentType.players())
								.then(CommandManager.argument("category", CategoryArgumentType.category())
										.then(CommandManager.argument("skill", SkillArgumentType.skillFromCategory("category"))
												.executes(SkillsCommand::unlock)
										)
								)
						)
				)
				.then(CommandManager.literal("lock")
						.then(CommandManager.argument("players", EntityArgumentType.players())
								.then(CommandManager.argument("category", CategoryArgumentType.category())
										.then(CommandManager.argument("skill", SkillArgumentType.skillFromCategory("category"))
												.executes(SkillsCommand::lock)
										)
								)
						)
				)
                                .then(CommandManager.literal("reset")
                                                .then(CommandManager.argument("players", EntityArgumentType.players())
                                                                .then(CommandManager.argument("category", CategoryArgumentType.category())
                                                                                .executes(SkillsCommand::reset)
                                                                )
                                                )
                                )
                                .then(CommandManager.literal("refund")
                                                .then(CommandManager.argument("players", EntityArgumentType.players())
                                                                .then(CommandManager.argument("category", CategoryArgumentType.category())
                                                                                .then(CommandManager.argument("skill", SkillArgumentType.skillFromCategory("category"))
                                                                                                .executes(SkillsCommand::refund)
                                                                                                .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                                                                                                                .executes(SkillsCommand::refundCount)
                                                                                                )
                                                                                                .then(CommandManager.literal("all")
                                                                                                                .executes(SkillsCommand::refundAll)
                                                                                                )
                                                                                )
                                                                )
                                                )
                                );
        }

	private static int unlock(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var players = EntityArgumentType.getPlayers(context, "players");
		var category = CategoryArgumentType.getCategory(context, "category");
		var skill = SkillArgumentType.getSkillFromCategory(context, "skill", category);

		for (var player : players) {
			skill.unlock(player);
		}
		CommandUtils.sendSuccess(
				context,
				players,
				"skills.unlock",
				category.getId(),
				skill.getId()
		);
		return players.size();
	}

	private static int lock(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var players = EntityArgumentType.getPlayers(context, "players");
		var category = CategoryArgumentType.getCategory(context, "category");
		var skill = SkillArgumentType.getSkillFromCategory(context, "skill", category);

		for (var player : players) {
			skill.lock(player);
		}
		CommandUtils.sendSuccess(
				context,
				players,
				"skills.lock",
				category.getId(),
				skill.getId()
		);
		return players.size();
	}

        private static int reset(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                var players = EntityArgumentType.getPlayers(context, "players");
                var category = CategoryArgumentType.getCategory(context, "category");

		for (var player : players) {
			category.resetSkills(player);
		}
		CommandUtils.sendSuccess(
				context,
				players,
				"skills.reset",
				category.getId()
		);
                return players.size();
        }

       private static int refund(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
               return refund(context, 1);
       }

       private static int refund(CommandContext<ServerCommandSource> context, int count) throws CommandSyntaxException {
               var players = EntityArgumentType.getPlayers(context, "players");
               var category = CategoryArgumentType.getCategory(context, "category");
               var skill = SkillArgumentType.getSkillFromCategory(context, "skill", category);

               var refunded = false;
               for (var player : players) {
                       for (var i = 0; i < count; i++) {
                               if (!SkillsMod.getInstance().refundSkill(player, category.getId(), skill.getId())) {
                                       break;
                               }
                               refunded = true;
                       }
               }

               if (refunded) {
                       if (count == 1) {
                               CommandUtils.sendSuccess(
                                               context,
                                               players,
                                               "skills.refund",
                                               skill.getId(),
                                               category.getId()
                               );
                       } else {
                               CommandUtils.sendSuccess(
                                               context,
                                               players,
                                               "skills.refund_many",
                                               count,
                                               skill.getId(),
                                               category.getId()
                               );
                       }
                       return players.size();
               } else {
                       context.getSource().sendError(SkillsMod.createTranslatable(
                                       "command",
                                       "skills.refund.no_levels",
                                       skill.getId(),
                                       category.getId()
                       ));
                       return 0;
               }
       }

       private static int refundCount(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
               var count = IntegerArgumentType.getInteger(context, "count");
               return refund(context, count);
       }

       private static int refundAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
               var players = EntityArgumentType.getPlayers(context, "players");
               var category = CategoryArgumentType.getCategory(context, "category");
               var skill = SkillArgumentType.getSkillFromCategory(context, "skill", category);

               var refunded = false;
               for (var player : players) {
                       while (SkillsMod.getInstance().refundSkill(player, category.getId(), skill.getId())) {
                               refunded = true;
                       }
               }

               if (refunded) {
                       CommandUtils.sendSuccess(
                                       context,
                                       players,
                                       "skills.refund_all",
                                       skill.getId(),
                                       category.getId()
                       );
                       return players.size();
               } else {
                       context.getSource().sendError(SkillsMod.createTranslatable(
                                       "command",
                                       "skills.refund.no_levels",
                                       skill.getId(),
                                       category.getId()
                       ));
                       return 0;
               }
       }
}
