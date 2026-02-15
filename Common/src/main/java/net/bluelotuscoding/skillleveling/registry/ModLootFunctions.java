package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.loot.function.LootFunctionType;

public class ModLootFunctions {
    public static final net.minecraft.util.Identifier RANDOMIZE_SKILL_TOME_ID = SkillLevelingMod
            .createIdentifier("randomize_skill_tome");
    public static final net.minecraft.util.Identifier SKILL_IMBUE_ID = SkillLevelingMod.createIdentifier("skill_imbue");

    // Holds the registered instances. Populated by platform-specific registries.
    public static LootFunctionType RANDOMIZE_SKILL_TOME;
    public static LootFunctionType SKILL_IMBUE;
}
