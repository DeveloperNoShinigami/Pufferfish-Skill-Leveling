package net.bluelotuscoding.puffishskillleveling.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.api.SkillsAPI;

public final class CategoryCommand {
    private CategoryCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("category")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("lock")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("category", StringArgumentType.string())
                                        .executes(context -> {
                                            var players = EntityArgument.getPlayers(context, "players");
                                            var categoryId = ResourceLocation.tryParse(StringArgumentType.getString(context, "category"));
                                            if (categoryId == null) {
                                                return 0;
                                            }
                                            SkillsAPI.getCategory(categoryId)
                                                    .ifPresent(category -> {
                                                        for (var player : players) {
                                                            category.lock(player);
                                                        }
                                                    });
                                            return players.size();
                                        }))
                        ))
                .then(Commands.literal("unlock")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("category", StringArgumentType.string())
                                        .executes(context -> {
                                            var players = EntityArgument.getPlayers(context, "players");
                                            var categoryId = ResourceLocation.tryParse(StringArgumentType.getString(context, "category"));
                                            if (categoryId == null) {
                                                return 0;
                                            }
                                            SkillsAPI.getCategory(categoryId)
                                                    .ifPresent(category -> {
                                                        for (var player : players) {
                                                            category.unlock(player);
                                                        }
                                                    });
                                            return players.size();
                                        }))
                        ))
                .then(Commands.literal("erase")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("category", StringArgumentType.string())
                                        .executes(context -> {
                                            var players = EntityArgument.getPlayers(context, "players");
                                            var categoryId = ResourceLocation.tryParse(StringArgumentType.getString(context, "category"));
                                            if (categoryId == null) {
                                                return 0;
                                            }
                                            SkillsAPI.getCategory(categoryId)
                                                    .ifPresent(category -> {
                                                        for (var player : players) {
                                                            category.erase(player);
                                                        }
                                                    });
                                            return players.size();
                                        }))
                        ));
    }
}

