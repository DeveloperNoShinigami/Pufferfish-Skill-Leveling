package net.puffish.skillsmod.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;

public class ExtendedSkillsCommand extends SkillsCommand {
	public static LiteralArgumentBuilder<ServerCommandSource> create() {
		var root = SkillsCommand.create();
		RefundCommandExtension.inject(root);
		return root;
	}
}

