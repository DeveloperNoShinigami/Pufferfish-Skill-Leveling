package net.puffish.skillsmod.main;

import net.puffish.skillsmod.client.addon.SkillLevelingAddonClient;

/**
 * Forge client bootstrap that builds upon the base implementation and
 * registers extra packet handlers and configuration for the Skill Leveling addon.
 */
public class ExtendedForgeClientMain extends ForgeClientMain {

    public ExtendedForgeClientMain() {
        super();
        SkillLevelingAddonClient.setup();
    }
}

