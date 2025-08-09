package net.bluelotuscoding.puffishskillleveling.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class RefundCommandExtension {
        public static void register() {
                SkillsCommand.REFUND_EXTENSION.register(builder -> {
                        builder.then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                                        .executes(ctx -> SkillsCommand.refund(ctx, IntegerArgumentType.getInteger(ctx, "count"))));
                        builder.then(CommandManager.literal("all")
                                        .executes(RefundCommandExtension::refundAll));
                });
        }

        private static int refundAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                return SkillsCommand.refundAll(context);
        }
}
