package net.bluelotuscoding.skillleveling.network;

import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.bluelotuscoding.skillleveling.bridge.forge.client.network.ForgeClientPacketHandlers;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

import java.util.Optional;
import net.bluelotuscoding.skillleveling.bridge.network.CustomChooseClassPacket;
import net.bluelotuscoding.skillleveling.bridge.network.CustomSyncClassPacket;
import net.bluelotuscoding.skillleveling.bridge.network.CustomAllocateStatPacket;
import net.bluelotuscoding.skillleveling.bridge.network.SyncCustomNbtPacket;
import net.bluelotuscoding.skillleveling.bridge.network.OpenAdvanceClassScreenPacket;

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

        CHANNEL.registerMessage(id++, CustomChooseClassPacket.class,
                CustomChooseClassPacket::encode, CustomChooseClassPacket::decode,
                CustomChooseClassPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, CustomSyncClassPacket.class,
                CustomSyncClassPacket::encode, CustomSyncClassPacket::decode,
                CustomSyncClassPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, CustomAllocateStatPacket.class,
                CustomAllocateStatPacket::encode, CustomAllocateStatPacket::decode,
                CustomAllocateStatPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, SyncCustomNbtPacket.class,
                SyncCustomNbtPacket::encode, SyncCustomNbtPacket::decode,
                SyncCustomNbtPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, OpenAdvanceClassScreenPacket.class,
                OpenAdvanceClassScreenPacket::encode, OpenAdvanceClassScreenPacket::decode,
                OpenAdvanceClassScreenPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, SyncBridgeContentPacket.class, SyncBridgeContentPacket::write,
                SyncBridgeContentPacket::read, (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    if (context.getDirection().getReceptionSide().isClient()) {
                        context.enqueueWork(() -> ForgeClientPacketHandlers.handleSyncBridgeContent(
                                packet.getClassDefinitions(),
                                packet.getClassAttributePages(),
                                packet.getSkillDisplayCache(),
                                packet.getConfig()
                        ));
                    }
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, SyncCustomClassPacket.class, SyncCustomClassPacket::write,
                SyncCustomClassPacket::read, (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    if (context.getDirection().getReceptionSide().isClient()) {
                        context.enqueueWork(() -> ForgeClientPacketHandlers.handleCustomSyncClass(
                                packet.getPlayerId(), packet.getClassId()
                        ));
                    }
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, SyncItemRestrictionsPacket.class, SyncItemRestrictionsPacket::encode,
                SyncItemRestrictionsPacket::decode, (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    if (context.getDirection().getReceptionSide().isClient()) {
                        context.enqueueWork(packet::handleClient);
                    }
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, SyncAllConfigsPacket.class, SyncAllConfigsPacket::write,
                SyncAllConfigsPacket::read, (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    if (context.getDirection().getReceptionSide().isClient()) {
                        context.enqueueWork(() -> ForgeClientPacketHandlers.handleSyncAllConfigs(
                                packet.getLeveledConfigs(),
                                packet.getExpTomeDefinitions()
                        ));
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
    public void sendToPlayer(SyncItemRestrictionsPacket packet, ServerPlayerEntity player) {
        CHANNEL.sendTo(packet, player.networkHandler.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @Override
    public void sendToPlayer(SyncAllConfigsPacket packet, ServerPlayerEntity player) {
        CHANNEL.sendTo(packet, player.networkHandler.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @Override
    public void sendToServer(RequestToggleSkillPacket packet) {
        CHANNEL.sendToServer(packet);
    }
}
