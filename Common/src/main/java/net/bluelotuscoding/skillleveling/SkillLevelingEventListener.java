package net.bluelotuscoding.skillleveling;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.puffish.skillsmod.server.event.ServerEventListener;
import net.bluelotuscoding.skillleveling.commands.SkillLevelingCommand;

/**
 * Event listener that hooks into core mod events to provide multi-level skill functionality
 */
public class SkillLevelingEventListener implements ServerEventListener {
    
    @Override
    public void onServerStarting(MinecraftServer server) {
        // Initialize any server-specific data
        SkillLevelingMod.getInstance().getSkillLevelingManager().onServerStarting(server);
    }

    @Override
    public void onServerReload(MinecraftServer server) {
        // Handle server reload events
        SkillLevelingMod.getInstance().getSkillLevelingManager().onServerReload(server);
    }

    @Override
    public void onPlayerJoin(ServerPlayerEntity player) {
        // Initialize player data for multi-level skills
        SkillLevelingMod.getInstance().getSkillLevelingManager().onPlayerJoin(player);
    }

    @Override
    public void onPlayerLeave(ServerPlayerEntity player) {
        // Save player data for multi-level skills
        SkillLevelingMod.getInstance().getSkillLevelingManager().onPlayerLeave(player);
    }

    @Override
    public void onCommandsRegister(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Register our addon commands under the main skills command
        dispatcher.register(CommandManager.literal("skillleveling")
                .then(SkillLevelingCommand.create())
        );
    }
}