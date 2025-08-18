package net.bluelotuscoding.skillleveling.main;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
	}
}