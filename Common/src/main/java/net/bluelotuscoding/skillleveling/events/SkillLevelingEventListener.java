package net.bluelotuscoding.skillleveling.events;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.puffish.skillsmod.server.event.ServerEventListener;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.commands.SkillLevelingCommand;

/**
 * Listener for skill-related events to integrate multi-level functionality
 */
public class SkillLevelingEventListener implements ServerEventListener {

    @Override
    public void onServerStarting(MinecraftServer server) {
        SkillLevelingMod.getInstance().getSkillLevelingManager().onServerStarting(server);
    }

    @Override
    public void onServerReload(MinecraftServer server) {
        SkillLevelingMod.getInstance().getSkillLevelingManager().onServerReload(server);
    }

    @Override
    public void onPlayerJoin(ServerPlayerEntity player) {
        SkillLevelingMod.getInstance().getSkillLevelingManager().onPlayerJoin(player);
    }

    @Override
    public void onPlayerLeave(ServerPlayerEntity player) {
        SkillLevelingMod.getInstance().getSkillLevelingManager().onPlayerLeave(player);
    }

    @Override
    public void onCommandsRegister(CommandDispatcher<ServerCommandSource> dispatcher) {
        SkillLevelingCommand.register(dispatcher);
    }
}
