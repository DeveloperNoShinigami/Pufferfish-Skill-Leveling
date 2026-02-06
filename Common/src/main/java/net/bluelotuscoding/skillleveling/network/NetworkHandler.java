package net.bluelotuscoding.skillleveling.network;

import net.minecraft.server.network.ServerPlayerEntity;

public interface NetworkHandler {
    void sendToPlayer(SyncSkillLevelPacket packet, ServerPlayerEntity player);

    void sendToPlayer(SyncSkillDescriptionsPacket packet, ServerPlayerEntity player);

    void sendToPlayer(CloseSkillScreenPacket packet, ServerPlayerEntity player);
}
