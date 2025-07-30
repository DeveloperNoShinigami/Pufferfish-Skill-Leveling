package net.puffish.skill_leveling.client.keybinding;

import net.minecraft.client.option.KeyBinding;

public interface KeyBindingReceiver {
	void registerKeyBinding(KeyBinding keyBinding, KeyBindingHandler handler);
}
