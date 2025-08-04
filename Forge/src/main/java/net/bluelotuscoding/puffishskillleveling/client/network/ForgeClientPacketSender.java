package net.bluelotuscoding.puffishskillleveling.client.network;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.puffish.skillsmod.client.network.ClientPacketSender;
import net.puffish.skillsmod.network.OutPacket;

/**
 * Forge implementation of the ClientPacketSender defined by the
 * Skills API. It forwards packets to the server using the Forge
 * networking utilities.
 */
public class ForgeClientPacketSender implements ClientPacketSender {
    @Override
    public void send(OutPacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(buf);
        Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(packet.getId(), buf));
    }
}
