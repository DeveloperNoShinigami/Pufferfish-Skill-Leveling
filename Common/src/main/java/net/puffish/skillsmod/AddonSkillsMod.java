package net.puffish.skillsmod;

import java.nio.file.Path;

import net.puffish.skillsmod.server.event.ServerEventReceiver;
import net.puffish.skillsmod.server.network.ServerPacketSender;
import net.puffish.skillsmod.server.setup.ServerPlatform;
import net.puffish.skillsmod.server.setup.ServerRegistrar;

/**
 * Addon entry point that delegates to the core SkillsMod.
 */
public class AddonSkillsMod extends SkillsMod {
    private AddonSkillsMod(Path modConfigDir, ServerPacketSender packetSender, ServerPlatform platform) {
        // delegate to the base constructor
        super(modConfigDir, packetSender, platform);
    }

    /**
     * Initializes the addon and the core mod.
     */
    public static void setup(
            Path configDir,
            ServerRegistrar registrar,
            ServerEventReceiver eventReceiver,
            ServerPacketSender packetSender,
            ServerPlatform platform
    ) {
        // Reuse the base mod's setup logic
        SkillsMod.setup(configDir, registrar, eventReceiver, packetSender, platform);
    }
}
