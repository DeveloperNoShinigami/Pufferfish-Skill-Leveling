package net.puffish.skill_leveling.client.setup;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skill_leveling.client.network.ClientPacketHandler;
import net.puffish.skill_leveling.network.InPacket;

import java.util.function.Function;

public interface ClientRegistrar {
	<T extends InPacket> void registerInPacket(Identifier id, Function<PacketByteBuf, T> reader, ClientPacketHandler<T> handler);
	void registerOutPacket(Identifier id);
}

