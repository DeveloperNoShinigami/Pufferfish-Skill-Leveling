package net.bluelotuscoding.puffishskillleveling.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public final class ModCommands {
    private ModCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(CategoryCommand.create());
        dispatcher.register(ExperienceCommand.create());
        dispatcher.register(PointsCommand.create());
        dispatcher.register(SkillsCommand.create());
    }
}

