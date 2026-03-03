package net.bluelotuscoding.skillleveling.network;

import net.minecraft.server.network.ServerPlayerEntity;

public interface NetworkHandler {
    void sendToPlayer(SyncBridgeContentPacket packet, ServerPlayerEntity player);

    void sendToPlayer(SyncCustomClassPacket packet, ServerPlayerEntity player);

    void sendToPlayer(SyncSkillLevelPacket packet, ServerPlayerEntity player);

    void sendToPlayer(SyncSkillDescriptionsPacket packet, ServerPlayerEntity player);

    void sendToPlayer(CloseSkillScreenPacket packet, ServerPlayerEntity player);

    void sendToPlayer(SyncToggleCooldownPacket packet, ServerPlayerEntity player);

    void sendToServer(RequestToggleSkillPacket packet);
}
