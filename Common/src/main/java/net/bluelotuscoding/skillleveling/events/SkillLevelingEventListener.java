package net.bluelotuscoding.skillleveling.events;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.puffish.skillsmod.server.event.ServerEventListener;
import net.bluelotuscoding.skillleveling.commands.SkillLevelingCommand;

/**
 * Event listener that handles server events for the skill leveling addon.
 */
public class SkillLevelingEventListener implements ServerEventListener {

    @Override
    public void onServerStarting(MinecraftServer server) {
        // Initialize any server-specific data when server starts
    }

    @Override
    public void onServerReload(MinecraftServer server) {
        // Handle server reload events - refresh configurations if needed
    }

    @Override
    public void onPlayerJoin(ServerPlayerEntity player) {
        // Initialize player-specific skill leveling data when they join
    }

    @Override
    public void onPlayerLeave(ServerPlayerEntity player) {
        // Clean up player-specific data when they leave
    }

    @Override
    public void onCommandsRegister(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Register our custom commands
        SkillLevelingCommand.register(dispatcher);
    }
}
