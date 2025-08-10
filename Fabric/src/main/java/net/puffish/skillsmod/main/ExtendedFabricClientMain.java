package net.puffish.skillsmod.main;

import net.puffish.skillsmod.client.addon.SkillLevelingAddonClient;

/**
 * Fabric entry point that extends the base client bootstrap to register
 * additional packets and configuration required by the Skill Leveling addon.
 */
public class ExtendedFabricClientMain extends FabricClientMain {

    @Override
    public void onInitializeClient() {
        super.onInitializeClient();
        SkillLevelingAddonClient.setup();
    }
}

