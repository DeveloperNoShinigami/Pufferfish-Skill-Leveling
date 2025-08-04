package net.bluelotuscoding.puffishskillleveling.reward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.util.LegacyUtils;

/**
 * Reward that runs commands when unlocked or locked.
 */
public class CommandReward implements Reward {
    public static final ResourceLocation ID = SkillsMod.createIdentifier("command");

    private final Map<UUID, Integer> counts = new HashMap<>();
    private final String command;
    private final String unlockCommand;
    private final String lockCommand;

    private CommandReward(String command, String unlockCommand, String lockCommand) {
        this.command = command;
        this.unlockCommand = unlockCommand;
        this.lockCommand = lockCommand;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, CommandReward::parse);
    }

    private static Result<CommandReward, Problem> parse(RewardConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(LegacyUtils.wrapNoUnused(CommandReward::parse, context));
    }

    private static Result<CommandReward, Problem> parse(JsonObject rootObject) {
        var problems = new ArrayList<Problem>();

        String command = rootObject.get("command")
                .getSuccess()
                .flatMap(e -> e.getAsString().ifFailure(problems::add).getSuccess())
                .orElse("");

        String unlockCommand = rootObject.get("unlock_command")
                .getSuccess()
                .flatMap(e -> e.getAsString().ifFailure(problems::add).getSuccess())
                .orElse("");

        String lockCommand = rootObject.get("lock_command")
                .getSuccess()
                .flatMap(e -> e.getAsString().ifFailure(problems::add).getSuccess())
                .orElse("");

        if (problems.isEmpty()) {
            return Result.success(new CommandReward(command, unlockCommand, lockCommand));
        } else {
            return Result.failure(Problem.combine(problems));
        }
    }

    private void executeCommand(ServerPlayer player, String command) {
        if (command.isBlank()) {
            return;
        }
        MinecraftServer server = Objects.requireNonNull(player.getServer());
        CommandSourceStack source = player.createCommandSourceStack()
                .withPermission(server.getFunctionCompilationLevel());
        server.getCommands().performPrefixedCommand(source, command);
    }

    @Override
    public void update(RewardUpdateContext context) {
        ServerPlayer player = context.getPlayer();
        if (context.isAction()) {
            executeCommand(player, command);
        }
        counts.compute(player.getUUID(), (uuid, count) -> {
            if (count == null) {
                count = 0;
            }
            while (context.getCount() > count) {
                executeCommand(player, unlockCommand);
                count++;
            }
            while (context.getCount() < count) {
                executeCommand(player, lockCommand);
                count--;
            }
            return count;
        });
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
            ServerPlayer player = context.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            for (int i = 0; i < entry.getValue(); i++) {
                executeCommand(player, lockCommand);
            }
        }
        counts.clear();
    }
}
