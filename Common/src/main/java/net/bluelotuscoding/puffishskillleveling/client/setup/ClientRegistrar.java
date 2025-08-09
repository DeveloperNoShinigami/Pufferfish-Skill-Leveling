package net.bluelotuscoding.puffishskillleveling.client.setup;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.client.network.ClientPacketHandler;
import net.bluelotuscoding.puffishskillleveling.network.InPacket;

import java.util.function.Function;

public interface ClientRegistrar {
	<T extends InPacket> void registerInPacket(Identifier id, Function<PacketByteBuf, T> reader, ClientPacketHandler<T> handler);
	void registerOutPacket(Identifier id);
}

