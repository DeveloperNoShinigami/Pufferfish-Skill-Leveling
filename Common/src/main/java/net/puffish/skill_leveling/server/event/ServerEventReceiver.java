package net.puffish.skill_leveling.server.event;

public interface ServerEventReceiver {
	void registerListener(ServerEventListener eventListener);
}
