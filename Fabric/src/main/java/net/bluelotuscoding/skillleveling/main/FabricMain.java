package net.bluelotuscoding.skillleveling.main;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.commands.SkillLevelingCommand;

/**
 * Fabric main class for the Skill Leveling addon
 */
public class FabricMain implements ModInitializer {

	@Override
	public void onInitialize() {
                SkillLevelingMod.init();

                // Register commands
                CommandRegistrationCallback.EVENT.register(
                        (dispatcher, registryAccess, environment) ->
                                SkillLevelingCommand.register(dispatcher)
                );

                ServerLifecycleEvents.SERVER_STARTING.register(server ->
                        SkillLevelingMod.getInstance().getSkillLevelingManager().onServerStarting(server)
                );
                ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                        SkillLevelingMod.getInstance().getSkillLevelingManager().onServerStopping(server)
                );
                ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                        SkillLevelingMod.getInstance().getSkillLevelingManager().onPlayerJoin(handler.player)
                );
                ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                        SkillLevelingMod.getInstance().getSkillLevelingManager().onPlayerLeave(handler.player)
                );
        }
}