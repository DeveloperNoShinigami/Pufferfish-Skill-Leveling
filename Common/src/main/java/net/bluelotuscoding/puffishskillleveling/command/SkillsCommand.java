package net.bluelotuscoding.puffishskillleveling.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.api.SkillsAPI;

public final class SkillsCommand {
    private SkillsCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("skills")
                .requires(source -> source.hasPermission(2))
                .then(
                        Commands.literal("unlock")
                                .then(
                                        Commands.argument("players", EntityArgument.players())
                                                .then(
                                                        Commands.argument("category", StringArgumentType.string())
                                                                .then(
                                                                        Commands.argument("skill", StringArgumentType.string())
                                                                                .executes(context -> {
                                                                                    var players = EntityArgument.getPlayers(context, "players");
                                                                                    var categoryId = ResourceLocation.tryParse(StringArgumentType.getString(context, "category"));
                                                                                    var skillId = StringArgumentType.getString(context, "skill");
                                                                                    if (categoryId == null) {
                                                                                        return 0;
                                                                                    }
                                                                                    SkillsAPI.getCategory(categoryId)
                                                                                            .flatMap(cat -> cat.getSkill(skillId))
                                                                                            .ifPresent(skill -> {
                                                                                                for (var player : players) {
                                                                                                    skill.unlock(player);
                                                                                                }
                                                                                            });
                                                                                    return players.size();
                                                                                })
                                                                )
                                                )
                                )
                )
                .then(
                        Commands.literal("reset")
                                .then(
                                        Commands.argument("players", EntityArgument.players())
                                                .then(
                                                        Commands.argument("category", StringArgumentType.string())
                                                                .executes(context -> {
                                                                    var players = EntityArgument.getPlayers(context, "players");
                                                                    var categoryId = ResourceLocation.tryParse(StringArgumentType.getString(context, "category"));
                                                                    if (categoryId == null) {
                                                                        return 0;
                                                                    }
                                                                    SkillsAPI.getCategory(categoryId)
                                                                            .ifPresent(category -> {
                                                                                for (var player : players) {
                                                                                    category.resetSkills(player);
                                                                                }
                                                                            });
                                                                    return players.size();
                                                                })
                                                )
                                )
                );
    }
}

