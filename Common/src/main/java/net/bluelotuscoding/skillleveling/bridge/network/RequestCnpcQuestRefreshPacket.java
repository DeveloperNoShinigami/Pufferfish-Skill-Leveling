package net.bluelotuscoding.skillleveling.bridge.network;

import net.minecraft.network.PacketByteBuf;

public class RequestCnpcQuestRefreshPacket {

    public static void encode(RequestCnpcQuestRefreshPacket msg, PacketByteBuf buf) {
    }

    public static RequestCnpcQuestRefreshPacket decode(PacketByteBuf buf) {
        return new RequestCnpcQuestRefreshPacket();
    }
}
