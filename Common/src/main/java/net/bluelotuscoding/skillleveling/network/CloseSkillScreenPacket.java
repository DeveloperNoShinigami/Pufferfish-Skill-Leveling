package net.bluelotuscoding.skillleveling.network;

import net.minecraft.network.PacketByteBuf;

/**
 * Tells the client to close the current screen (e.g. when a skill cannot be
 * unlocked).
 */
public class CloseSkillScreenPacket {

    public CloseSkillScreenPacket() {
    }

    public static CloseSkillScreenPacket decode(PacketByteBuf buf) {
        return new CloseSkillScreenPacket();
    }

    public void encode(PacketByteBuf buf) {
        // Empty packet
    }

    public void handleClient() {
        net.bluelotuscoding.skillleveling.client.ClientPacketHandler.handleCloseScreen();
    }
}
