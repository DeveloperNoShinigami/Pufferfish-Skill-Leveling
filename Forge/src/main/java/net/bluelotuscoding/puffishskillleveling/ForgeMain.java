package net.bluelotuscoding.puffishskillleveling;

import net.minecraftforge.fml.common.Mod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.bluelotuscoding.puffishskillleveling.experience.source.ExperienceEvents;

@Mod(PuffishSkillLeveling.MOD_ID)
public final class ForgeMain {
    public ForgeMain() {
        // Touch the Skills API to ensure the dependency is required at compile time.
        SkillsAPI.MOD_ID.hashCode();

        // Register addon features.
        PuffishSkillLeveling.init();
        ExperienceEvents.register();
    }
}
