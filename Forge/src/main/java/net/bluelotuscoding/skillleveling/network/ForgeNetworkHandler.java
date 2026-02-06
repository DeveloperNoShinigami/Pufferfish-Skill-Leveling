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
                    if (context.getDirection().getReceptionSide().isClient()) {
                        context.enqueueWork(packet::handleClient);
                    }
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, SyncSkillDescriptionsPacket.class, SyncSkillDescriptionsPacket::encode,
                SyncSkillDescriptionsPacket::decode, (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    if (context.getDirection().getReceptionSide().isClient()) {
                        context.enqueueWork(packet::handleClient);
                    }
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, CloseSkillScreenPacket.class, CloseSkillScreenPacket::encode,
                CloseSkillScreenPacket::decode, (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    if (context.getDirection().getReceptionSide().isClient()) {
                        context.enqueueWork(packet::handleClient);
                    }
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // Client-to-server packet for Tome actions
        CHANNEL.registerMessage(id++, TomeActionPacket.class, TomeActionPacket::write,
                TomeActionPacket::read, (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> {
                        var sender = context.getSender();
                        if (sender != null) {
                            SkillLevelingMod.getInstance().getSkillLevelingManager()
                                    .processTomeAction(sender, packet.getCategoryId(),
                                            packet.getSkillId(), packet.getTomeType());
                        }
                    });
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    @Override
    public void sendToPlayer(SyncSkillLevelPacket packet, ServerPlayerEntity player) {
        try {
            SkillLevelingMod.getInstance().getLogger()
                    .debug("Sending SyncSkillLevelPacket to " + player.getName().getString());
        } catch (Exception ignored) {
        }
        CHANNEL.sendTo(packet, player.networkHandler.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @Override
    public void sendToPlayer(SyncSkillDescriptionsPacket packet, ServerPlayerEntity player) {
        try {
            SkillLevelingMod.getInstance().getLogger()
                    .debug("Sending SyncSkillDescriptionsPacket to " + player.getName().getString()
                            + " -> " + packet.getDefinitionId() + " (levels=" + packet.getLevelDescriptions().size()
                            + ", extras="
                            + packet.getLevelExtraDescriptions().size() + ")");
        } catch (Exception ignored) {
        }
        CHANNEL.sendTo(packet, player.networkHandler.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @Override
    public void sendToPlayer(CloseSkillScreenPacket packet, ServerPlayerEntity player) {
        CHANNEL.sendTo(packet, player.networkHandler.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}
