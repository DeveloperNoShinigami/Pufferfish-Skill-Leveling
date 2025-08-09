package net.bluelotuscoding.puffishskillleveling.client;

import net.bluelotuscoding.puffishskillleveling.client.network.ClientPacketSender;
import net.bluelotuscoding.puffishskillleveling.client.setup.ClientRegistrar;

public class SkillsClientMod {
    private static SkillsClientMod instance;

    private final ClientPacketSender packetSender;

    private SkillsClientMod(ClientPacketSender packetSender) {
        this.packetSender = packetSender;
    }

    public static void setup(ClientRegistrar registrar, ClientPacketSender packetSender) {
        instance = new SkillsClientMod(packetSender);
    }

    public static SkillsClientMod getInstance() {
        return instance;
    }

    public ClientPacketSender getPacketSender() {
        return packetSender;
    }
}

