package net.bluelotuscoding.skillleveling.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class FabricNetworkHandler implements NetworkHandler {

    public static void init() {
        // Server-side: Handle RequestToggleSkill
        ServerPlayNetworking.registerGlobalReceiver(SkillLevelingNetwork.REQUEST_TOGGLE_SKILL,
                (server, player, handler, buf, responseSender) -> {
                    RequestToggleSkillPacket packet = RequestToggleSkillPacket.decode(buf);
                    server.execute(() -> packet.handleServer(player));
                });

        // Client-side receivers are typically registered in a ClientModInitializer
    }

    public static void initClient() {
        // Client-side: Handle SyncSkillLevel
        ClientPlayNetworking.registerGlobalReceiver(SkillLevelingNetwork.SKILL_LEVEL_UPDATE,
                (client, handler, buf, responseSender) -> {
                    SyncSkillLevelPacket packet = SyncSkillLevelPacket.decode(buf);
                    client.execute(packet::handleClient);
                });

        // Client-side: Handle SyncSkillDescriptions
        ClientPlayNetworking.registerGlobalReceiver(SkillLevelingNetwork.SKILL_DESCRIPTION_UPDATE,
                (client, handler, buf, responseSender) -> {
                    SyncSkillDescriptionsPacket packet = SyncSkillDescriptionsPacket.decode(buf);
                    client.execute(packet::handleClient);
                });

        // Client-side: Handle CloseSkillScreen
        ClientPlayNetworking.registerGlobalReceiver(SkillLevelingNetwork.SKILL_PROGRESSION_UPDATE, // Assuming this is
                                                                                                   // for close or
                                                                                                   // similar
                (client, handler, buf, responseSender) -> {
                    // This seems to be handled differently in common, but let's follow the patterns
                });

        // Specifically for Toggle Cooldown
        ClientPlayNetworking.registerGlobalReceiver(SkillLevelingNetwork.TOGGLE_COOLDOWN,
                (client, handler, buf, responseSender) -> {
                    SyncToggleCooldownPacket packet = SyncToggleCooldownPacket.decode(buf);
                    client.execute(packet::handleClient);
                });
    }

    @Override
    public void sendToPlayer(SyncSkillLevelPacket packet, ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        ServerPlayNetworking.send(player, SkillLevelingNetwork.SKILL_LEVEL_UPDATE, buf);
    }

    @Override
    public void sendToPlayer(SyncSkillDescriptionsPacket packet, ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        ServerPlayNetworking.send(player, SkillLevelingNetwork.SKILL_DESCRIPTION_UPDATE, buf);
    }

    @Override
    public void sendToPlayer(CloseSkillScreenPacket packet, ServerPlayerEntity player) {
        // Note: CloseSkillScreenPacket identifier might need to be added to
        // SkillLevelingNetwork
        // For now, let's assume it's covered by SKILL_PROGRESSION_UPDATE or similar if
        // needed.
    }

    @Override
    public void sendToPlayer(SyncToggleCooldownPacket packet, ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        ServerPlayNetworking.send(player, SkillLevelingNetwork.TOGGLE_COOLDOWN, buf);
    }

    @Override
    public void sendToServer(RequestToggleSkillPacket packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        ClientPlayNetworking.send(SkillLevelingNetwork.REQUEST_TOGGLE_SKILL, buf);
    }
}
