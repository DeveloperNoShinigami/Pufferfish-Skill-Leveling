package net.puffish.skillsmod;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;

public class SkillsMod {
    public static final int MIN_CONFIG_VERSION = 1;
    public static final int MAX_CONFIG_VERSION = 3;
    public static Identifier createIdentifier(String path) {
        return new Identifier(SkillsAPI.MOD_ID, path);
    }
}
