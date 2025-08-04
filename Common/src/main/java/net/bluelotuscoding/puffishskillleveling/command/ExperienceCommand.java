package net.bluelotuscoding.puffishskillleveling.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.api.SkillsAPI;

public final class ExperienceCommand {
    private ExperienceCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("experience")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("category", StringArgumentType.string())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(context -> {
                                                    var players = EntityArgument.getPlayers(context, "players");
                                                    var categoryId = ResourceLocation.tryParse(StringArgumentType.getString(context, "category"));
                                                    var amount = IntegerArgumentType.getInteger(context, "amount");
                                                    if (categoryId == null) {
                                                        return 0;
                                                    }
                                                    SkillsAPI.getCategory(categoryId)
                                                            .flatMap(cat -> cat.getExperience())
                                                            .ifPresent(exp -> {
                                                                for (var player : players) {
                                                                    exp.addTotal(player, amount);
                                                                }
                                                            });
                                                    return players.size();
                                                }))
                                ))
                )
                .then(Commands.literal("set")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("category", StringArgumentType.string())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(context -> {
                                                    var players = EntityArgument.getPlayers(context, "players");
                                                    var categoryId = ResourceLocation.tryParse(StringArgumentType.getString(context, "category"));
                                                    var amount = IntegerArgumentType.getInteger(context, "amount");
                                                    if (categoryId == null) {
                                                        return 0;
                                                    }
                                                    SkillsAPI.getCategory(categoryId)
                                                            .flatMap(cat -> cat.getExperience())
                                                            .ifPresent(exp -> {
                                                                for (var player : players) {
                                                                    exp.setTotal(player, amount);
                                                                }
                                                            });
                                                    return players.size();
                                                }))
                                ))
                );
    }
}

