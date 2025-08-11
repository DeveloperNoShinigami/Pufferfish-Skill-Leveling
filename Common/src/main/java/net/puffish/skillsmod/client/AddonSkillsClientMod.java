package net.puffish.skillsmod.client;

import net.puffish.skillsmod.client.event.ClientEventReceiver;
import net.puffish.skillsmod.client.keybinding.KeyBindingReceiver;
import net.puffish.skillsmod.client.network.ClientPacketSender;
import net.puffish.skillsmod.client.setup.ClientRegistrar;

/**
 * Client-side addon entry point that delegates to the core SkillsClientMod.
 */
public class AddonSkillsClientMod extends SkillsClientMod {
    private AddonSkillsClientMod(ClientPacketSender packetSender) {
        super(packetSender);
    }

    public static void setup(
            ClientRegistrar registrar,
            ClientEventReceiver eventReceiver,
            KeyBindingReceiver keyBindingReceiver,
            ClientPacketSender packetSender
    ) {
        // Delegate to the base mod's setup method
        SkillsClientMod.setup(registrar, eventReceiver, keyBindingReceiver, packetSender);
    }
}
