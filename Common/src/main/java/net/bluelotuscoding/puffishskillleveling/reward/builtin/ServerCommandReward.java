package net.bluelotuscoding.puffishskillleveling.reward.builtin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.SkillsMod;
import net.bluelotuscoding.puffishskillleveling.api.SkillsAPI;
import net.bluelotuscoding.puffishskillleveling.api.reward.Reward;
import net.bluelotuscoding.puffishskillleveling.api.reward.RewardDisposeContext;
import net.bluelotuscoding.puffishskillleveling.api.reward.RewardUpdateContext;

import java.util.Objects;

public class ServerCommandReward implements Reward {
    public static final Identifier ID = SkillsMod.createIdentifier("server_command");

    private final CommandReward delegate;

    private ServerCommandReward(CommandReward delegate) {
        this.delegate = delegate;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, context ->
                CommandReward.parse(context, ServerCommandReward::executeAsServer)
                        .mapSuccess(ServerCommandReward::new)
        );
    }

    private static void executeAsServer(ServerPlayerEntity player, String command) {
        var server = Objects.requireNonNull(player.getServer());
        server.getCommandManager().executeWithPrefix(
                server.getCommandSource()
                        .withSilent()
                        .withLevel(server.getFunctionPermissionLevel()),
                command
        );
    }

    @Override
    public void update(RewardUpdateContext context) {
        delegate.update(context);
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        delegate.dispose(context);
    }
}
