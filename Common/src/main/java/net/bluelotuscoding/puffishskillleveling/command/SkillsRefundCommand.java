package net.bluelotuscoding.puffishskillleveling.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.commands.arguments.CategoryArgumentType;
import net.puffish.skillsmod.commands.arguments.SkillArgumentType;
import net.puffish.skillsmod.util.CommandUtils;

import java.util.Collection;

/**
 * Command that refunds levels from a skill for one or more players.
 */
public final class SkillsRefundCommand {
    private SkillsRefundCommand() {
    }

    /**
     * Builds the {@code refund} subcommand for the {@code skills} command.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("refund")
                .then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.argument("category", CategoryArgumentType.category())
                                .then(Commands.argument("skill", SkillArgumentType.skillFromCategory("category"))
                                        .executes(ctx -> refund(ctx, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                .executes(ctx -> refund(ctx, IntegerArgumentType.getInteger(ctx, "count"))))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> refund(ctx, -1)))
                                )));
    }

    private static int refund(CommandContext<CommandSourceStack> context, int count) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        Category category = CategoryArgumentType.getCategory(context, "category");
        Skill skill = SkillArgumentType.getSkillFromCategory(context, "skill", category);

        int total = 0;
        for (ServerPlayer player : players) {
            total += SkillsRefundHelper.refundSkill(player, skill, count);
        }

        if (total == 0) {
            context.getSource().sendFailure(SkillsMod.createTranslatable("command",
                    "puffish_skills.skills.refund.no_levels",
                    skill.getId(), category.getId()));
            return 0;
        }

        if (count < 0) {
            CommandUtils.sendSuccess(context, players, "skills.refund_all",
                    skill.getId(), category.getId());
        } else if (count == 1) {
            CommandUtils.sendSuccess(context, players, "skills.refund",
                    skill.getId(), category.getId());
        } else {
            CommandUtils.sendSuccess(context, players, "skills.refund_many",
                    count, skill.getId(), category.getId());
        }
        return total;
    }

    /**
     * Attaches this subcommand to the existing {@code /puffish_skills skills} command tree.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = dispatcher.getRoot().getChild("puffish_skills");
        if (root != null) {
            var skills = root.getChild("skills");
            if (skills != null) {
                skills.addChild(create().build());
            }
        }
    }
}
