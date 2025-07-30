package net.puffish.skill_leveling.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.puffish.skill_leveling.api.SkillsAPI;
import net.puffish.skill_leveling.util.CommandUtils;

public class OpenCommand {
	public static LiteralArgumentBuilder<ServerCommandSource> create() {
		return CommandManager.literal("open")
				.requires(source -> source.hasPermissionLevel(2))
				.then(CommandManager.argument("players", EntityArgumentType.players())
						.executes(OpenCommand::open)
				);
	}

	private static int open(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var players = EntityArgumentType.getPlayers(context, "players");

		for (var player : players) {
			SkillsAPI.openScreen(player);
		}
		CommandUtils.sendSuccess(
				context,
				players,
				"open"
		);
		return players.size();
	}
}
