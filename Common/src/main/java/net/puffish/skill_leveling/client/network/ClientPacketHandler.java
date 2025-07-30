package net.puffish.skill_leveling.client.network;

public interface ClientPacketHandler<T> {
	void handle(T packet);
}
