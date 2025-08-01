package net.puffish.skillsmod.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
                                                                                                .executes(ctx -> refund(ctx, false))
                                                                                                .then(CommandManager.literal("all")
                                                                                                                .executes(ctx -> refund(ctx, true))
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

       private static int refund(CommandContext<ServerCommandSource> context, boolean all) throws CommandSyntaxException {
               var players = EntityArgumentType.getPlayers(context, "players");
               var category = CategoryArgumentType.getCategory(context, "category");
               var skill = SkillArgumentType.getSkillFromCategory(context, "skill", category);

               var refunded = false;
               for (var player : players) {
                       refunded |= SkillsMod.getInstance().refundSkill(player, category.getId(), skill.getId(), all);
               }

               if (refunded) {
                       CommandUtils.sendSuccess(
                                       context,
                                       players,
                                       all ? "skills.refund_all" : "skills.refund",
                                       category.getId(),
                                       skill.getId()
                       );
                       return players.size();
               } else {
                       context.getSource().sendError(SkillsMod.createTranslatable(
                                       "command",
                                       "skills.refund.no_levels",
                                       category.getId(),
                                       skill.getId()
                       ));
                       return 0;
               }
       }
}
