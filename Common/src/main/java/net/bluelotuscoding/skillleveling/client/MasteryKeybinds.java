package net.bluelotuscoding.skillleveling.client;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

import java.util.ArrayList;
import java.util.List;

public class MasteryKeybinds {
    public static final String CATEGORY = "category.skillleveling.mastery";
    public static final List<KeyBinding> KEYBINDINGS = new ArrayList<>();

    public static void init() {
        for (int i = 1; i <= 9; i++) {
            KEYBINDINGS.add(new KeyBinding(
                    "key.skillleveling.mastery_" + i,
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    CATEGORY));
        }
    }

    public static void onClientTick() {
        for (int i = 0; i < KEYBINDINGS.size(); i++) {
            KeyBinding key = KEYBINDINGS.get(i);
            while (key.wasPressed()) {
                handleMasteryKey(i + 1);
            }
        }
    }

    public static void handleMasteryKey(int slot) {
        String skillKey = ClientSkillLevelStorage.getSkillKeyByKeybindSlot(slot);
        if (skillKey != null) {
            // FIX: Split by LAST colon to separate Category (which may contain colons) from
            // SkillID
            int lastColon = skillKey.lastIndexOf(':');
            if (lastColon > 0) {
                String categoryStr = skillKey.substring(0, lastColon);
                String skillId = skillKey.substring(lastColon + 1);

                net.minecraft.util.Identifier categoryId = net.minecraft.util.Identifier.tryParse(categoryStr);

                SkillLevelingMod.getInstance().getLogger().info("[MasteryKeybinds] Key press slot " + slot +
                        " -> Raw: " + skillKey +
                        " -> Parsed Cat: " + categoryId +
                        ", Skill: " + skillId);

                if (categoryId != null) {
                    net.bluelotuscoding.skillleveling.network.SkillLevelingNetwork.sendRequestToggleSkill(categoryId,
                            skillId);
                }
            }
        }
    }
}
