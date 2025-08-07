package net.bluelotuscoding.puffishskillleveling.client.network;

public interface ClientPacketHandler<T> {
	void handle(T packet);
}
