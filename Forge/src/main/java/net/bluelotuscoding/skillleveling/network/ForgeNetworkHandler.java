package net.bluelotuscoding.skillleveling.network;

import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

import java.util.Optional;

public class ForgeNetworkHandler implements NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new Identifier(SkillLevelingMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, SyncSkillLevelPacket.class, SyncSkillLevelPacket::encode,
                SyncSkillLevelPacket::decode, (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(packet::handleClient);
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    @Override
    public void sendToPlayer(SyncSkillLevelPacket packet, ServerPlayerEntity player) {
        CHANNEL.sendTo(packet, player.networkHandler.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}
