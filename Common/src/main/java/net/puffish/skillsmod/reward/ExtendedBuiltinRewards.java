package net.puffish.skillsmod.reward;

import net.puffish.skillsmod.reward.builtin.ExtendedCommandReward;

/**
 * Registers the default rewards along with additional extended reward types.
 * This allows addons to provide new built-in rewards without modifying the
 * original registration code.
 */
public class ExtendedBuiltinRewards extends BuiltinRewards {
    /**
     * Registers both the standard and extended reward implementations.
     */
    public static void register() {
        BuiltinRewards.register();
        ExtendedCommandReward.register();
    }
}
