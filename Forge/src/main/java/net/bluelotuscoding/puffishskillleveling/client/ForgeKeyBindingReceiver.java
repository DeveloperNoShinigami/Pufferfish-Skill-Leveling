package net.bluelotuscoding.puffishskillleveling.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.puffish.skillsmod.client.keybinding.KeyBindingHandler;
import net.puffish.skillsmod.client.keybinding.KeyBindingReceiver;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;

import java.util.Arrays;

/**
 * Registers key bindings and invokes handlers when the keys are
 * pressed.
 */
public class ForgeKeyBindingReceiver implements KeyBindingReceiver {
    @Override
    public void registerKeyBinding(KeyMapping key, KeyBindingHandler handler) {
        Minecraft mc = Minecraft.getInstance();
        KeyMapping[] old = mc.options.keyMappings;
        KeyMapping[] arr = Arrays.copyOf(old, old.length + 1);
        arr[old.length] = key;
        mc.options.keyMappings = arr;
        MinecraftForge.EVENT_BUS.addListener((ClientTickEvent event) -> {
            while (key.consumeClick()) {
                handler.handle();
            }
        });
    }
}
