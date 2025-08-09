package net.bluelotuscoding.puffishskillleveling.server.event;

public interface ServerEventReceiver {
	void registerListener(ServerEventListener eventListener);
}
