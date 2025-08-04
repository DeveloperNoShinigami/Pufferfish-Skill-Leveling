package net.bluelotuscoding.puffishskillleveling;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.bluelotuscoding.puffishskillleveling.command.SkillsRefundCommand;

@Mod(PuffishSkillLeveling.MOD_ID)
public final class ForgeMain {
    public ForgeMain() {
        // Touch the Skills API to ensure the dependency is required at compile time.
        SkillsAPI.MOD_ID.hashCode();

        // Register addon features.
        PuffishSkillLeveling.init();

        // Hook command registration to extend the /puffish_skills command tree.
        MinecraftForge.EVENT_BUS.addListener(ForgeMain::registerCommands);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        SkillsRefundCommand.register(event.getDispatcher());
    }
}
