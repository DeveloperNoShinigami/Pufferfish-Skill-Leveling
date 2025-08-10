package net.puffish.skillsmod.calculation.operation;

import net.puffish.skillsmod.calculation.operation.builtin.ItemStackNbtCondition;

/**
 * Registers the default operations supplied by the base mod and adds the
 * additional operations required by this project.
 */
public final class ExtendedBuiltinOperations extends BuiltinOperations {

    private ExtendedBuiltinOperations() {
    }

    public static void register() {
        BuiltinOperations.register();
        ItemStackNbtCondition.register();
    }
}

