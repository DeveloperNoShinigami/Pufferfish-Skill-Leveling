package net.bluelotuscoding.puffishskillleveling.experience.source;

/**
 * Registers all builtin experience sources provided by this addon.
 */
public final class BuiltinExperienceSources {
    public static void register() {
        BreakBlockExperienceSource.register();
        MineBlockExperienceSource.register();
    }

    private BuiltinExperienceSources() {
    }
}

