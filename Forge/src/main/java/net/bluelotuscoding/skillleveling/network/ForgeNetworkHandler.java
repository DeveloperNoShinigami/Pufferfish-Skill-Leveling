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

        CHANNEL.registerMessage(id++, RequestToggleSkillPacket.class, RequestToggleSkillPacket::encode,
                RequestToggleSkillPacket::decode, (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    if (context.getDirection().getReceptionSide().isServer()) {
                        context.enqueueWork(() -> {
                            var sender = context.getSender();
                            if (sender != null) {
                                packet.handleServer(sender);
                            }
                        });
                    }
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                net.bluelotuscoding.skillleveling.bridge.network.CustomChooseClassPacket.class,
                net.bluelotuscoding.skillleveling.bridge.network.CustomChooseClassPacket::encode,
                net.bluelotuscoding.skillleveling.bridge.network.CustomChooseClassPacket::decode,
                net.bluelotuscoding.skillleveling.bridge.network.CustomChooseClassPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                net.bluelotuscoding.skillleveling.bridge.network.CustomSyncClassPacket.class,
                net.bluelotuscoding.skillleveling.bridge.network.CustomSyncClassPacket::encode,
                net.bluelotuscoding.skillleveling.bridge.network.CustomSyncClassPacket::decode,
                net.bluelotuscoding.skillleveling.bridge.network.CustomSyncClassPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++,
                net.bluelotuscoding.skillleveling.bridge.network.CustomAllocateStatPacket.class,
                net.bluelotuscoding.skillleveling.bridge.network.CustomAllocateStatPacket::encode,
                net.bluelotuscoding.skillleveling.bridge.network.CustomAllocateStatPacket::decode,
                net.bluelotuscoding.skillleveling.bridge.network.CustomAllocateStatPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                net.bluelotuscoding.skillleveling.bridge.network.SyncCustomNbtPacket.class,
                net.bluelotuscoding.skillleveling.bridge.network.SyncCustomNbtPacket::encode,
                net.bluelotuscoding.skillleveling.bridge.network.SyncCustomNbtPacket::decode,
                net.bluelotuscoding.skillleveling.bridge.network.SyncCustomNbtPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++,
                net.bluelotuscoding.skillleveling.bridge.network.OpenAdvanceClassScreenPacket.class,
                net.bluelotuscoding.skillleveling.bridge.network.OpenAdvanceClassScreenPacket::encode,
                net.bluelotuscoding.skillleveling.bridge.network.OpenAdvanceClassScreenPacket::decode,
                net.bluelotuscoding.skillleveling.bridge.network.OpenAdvanceClassScreenPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, SyncBridgeContentPacket.class, SyncBridgeContentPacket::write,
                SyncBridgeContentPacket::read, (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    if (context.getDirection().getReceptionSide().isClient()) {
                        context.enqueueWork(() -> {
                            net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager
                                    .setClassesOnClient(packet.getClassDefinitions());
                            net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager
                                    .setAttributePagesOnClient(packet.getClassAttributePages());
                        });
                    }
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, SyncCustomClassPacket.class, SyncCustomClassPacket::write,
                SyncCustomClassPacket::read, (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    if (context.getDirection().getReceptionSide().isClient()) {
                        context.enqueueWork(() -> {
                            net.bluelotuscoding.skillleveling.client.ClientCustomClassState
                                    .setCustomClass(packet.getPlayerId(), packet.getClassId());
                        });
                    }
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

    }

    @Override
    public void sendToPlayer(SyncBridgeContentPacket packet, ServerPlayerEntity player) {
        CHANNEL.sendTo(packet, player.networkHandler.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @Override
    public void sendToPlayer(SyncCustomClassPacket packet, ServerPlayerEntity player) {
        CHANNEL.sendTo(packet, player.networkHandler.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @Override
    public void sendToPlayer(SyncSkillLevelPacket packet, ServerPlayerEntity player) {
        CHANNEL.sendTo(packet, player.networkHandler.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @Override
    public void sendToPlayer(SyncSkillDescriptionsPacket packet, ServerPlayerEntity player) {
        CHANNEL.sendTo(packet, player.networkHandler.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @Override
    public void sendToPlayer(CloseSkillScreenPacket packet, ServerPlayerEntity player) {
        CHANNEL.sendTo(packet, player.networkHandler.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @Override
    public void sendToPlayer(SyncToggleCooldownPacket packet, ServerPlayerEntity player) {
        CHANNEL.sendTo(packet, player.networkHandler.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @Override
    public void sendToServer(RequestToggleSkillPacket packet) {
        CHANNEL.sendToServer(packet);
    }
}
