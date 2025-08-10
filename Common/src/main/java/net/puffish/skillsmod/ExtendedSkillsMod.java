package net.puffish.skillsmod;

import net.minecraft.server.MinecraftServer;
import net.puffish.skillsmod.calculation.operation.ExtendedBuiltinOperations;
import net.puffish.skillsmod.network.Packets;
import net.puffish.skillsmod.server.event.ServerEventReceiver;
import net.puffish.skillsmod.server.network.ServerPacketSender;
import net.puffish.skillsmod.server.setup.ServerPlatform;
import net.puffish.skillsmod.server.setup.ServerRegistrar;

import java.nio.file.Path;

/**
 * Extension of the base {@link SkillsMod} that wires in the additional
 * functionality required by this project.  Custom configuration handling and
 * network packet registration are exposed through overridable hooks so the
 * upstream mod can remain untouched.
 */
public class ExtendedSkillsMod extends SkillsMod {

    protected ExtendedSkillsMod(Path modConfigDir, ServerPacketSender packetSender, ServerPlatform platform) {
        super(modConfigDir, packetSender, platform);
    }

    /**
     * Bootstraps the mod using this extended implementation.
     */
    public static void setup(
            Path configDir,
            ServerRegistrar registrar,
            ServerEventReceiver eventReceiver,
            ServerPacketSender packetSender,
            ServerPlatform platform
    ) {
        SkillsMod.setup(configDir, registrar, eventReceiver, packetSender, platform, ExtendedSkillsMod::new);
    }

    @Override
    protected void registerPackets(ServerRegistrar registrar) {
        super.registerPackets(registrar);
        registrar.registerOutPacket(Packets.NEW_POINT);
    }

    @Override
    protected void registerBuiltinOperations() {
        ExtendedBuiltinOperations.register();
    }

    @Override
    protected void copyConfigFromJar() {
        super.copyConfigFromJar();
        // Additional configuration files could be copied here if required
    }

    @Override
    protected void loadModConfig(MinecraftServer server) {
        super.loadModConfig(server);
        // Custom configuration parsing can be added here in the future
    }
}

