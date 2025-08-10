package net.puffish.skillsmod.reward.builtin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.util.LegacyUtils;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A variant of {@link CommandReward} that executes commands from the server
 * instead of the player. Useful for rewards that require elevated privileges
 * or should not depend on the player's command permissions.
 */
public class ExtendedCommandReward extends CommandReward {
    public static final Identifier ID = SkillsMod.createIdentifier("server_command");

    private ExtendedCommandReward(String command, String unlockCommand, String lockCommand) {
        super(command, unlockCommand, lockCommand);
    }

    public static void register() {
        SkillsAPI.registerReward(ID, ExtendedCommandReward::parse);
    }

    private static Result<ExtendedCommandReward, Problem> parse(RewardConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(LegacyUtils.wrapNoUnused(ExtendedCommandReward::parse, context));
    }

    private static Result<ExtendedCommandReward, Problem> parse(JsonObject rootObject) {
        var problems = new ArrayList<Problem>();

        var command = rootObject.get("command")
                .getSuccess()
                .flatMap(element -> element.getAsString().ifFailure(problems::add).getSuccess())
                .orElse("");

        var unlockCommand = rootObject.get("unlock_command")
                .getSuccess()
                .flatMap(element -> element.getAsString().ifFailure(problems::add).getSuccess())
                .orElse("");

        var lockCommand = rootObject.get("lock_command")
                .getSuccess()
                .flatMap(element -> element.getAsString().ifFailure(problems::add).getSuccess())
                .orElse("");

        if (problems.isEmpty()) {
            return Result.success(new ExtendedCommandReward(command, unlockCommand, lockCommand));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    @Override
    protected void executeCommand(ServerPlayerEntity player, String command) {
        if (command.isBlank()) {
            return;
        }

        var server = Objects.requireNonNull(player.getServer());
        server.getCommandManager().executeWithPrefix(
                server.getCommandSource()
                        .withSilent()
                        .withLevel(server.getFunctionPermissionLevel()),
                command
        );
    }
}
