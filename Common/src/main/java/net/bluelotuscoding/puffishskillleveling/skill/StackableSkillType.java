package net.bluelotuscoding.puffishskillleveling.skill;

import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.SkillsMod;

/**
 * Placeholder skill type that mirrors the behaviour of the legacy
 * stackable skills from the 1.20 branch. The identifier is reserved so
 * datapacks can reference {@code puffish_skills:stackable} and receive the
 * expected stacking behaviour provided by the base mod.
 *
 * <p>The base Skills API currently handles the stacking mechanics
 * internally, so registering the type only exposes a stable identifier
 * and translation entry.</p>
 */
public final class StackableSkillType {
    /** Identifier used by datapacks to refer to this skill type. */
    public static final ResourceLocation ID = SkillsMod.createIdentifier("stackable");

    private StackableSkillType() {
        // utility class
    }

    /**
     * Registers the stackable skill type.  The underlying skillsmod does not
     * currently expose a formal skill type registry, so this method simply
     * exists for symmetry with other features and future extensibility.
     */
    public static void register() {
        // Intentionally left blank; stacking behaviour is built into skillsmod.
    }
}

