package net.bluelotuscoding.puffishskillleveling.command;

import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.Skill;

/**
 * Utility for decrementing skill levels when refunding points.
 */
public final class SkillsRefundHelper {
    private SkillsRefundHelper() {
    }

    /**
     * Refunds a number of levels from the given skill.
     *
     * @param player player whose skill should be decremented
     * @param skill skill to modify
     * @param count number of levels to refund, or negative to refund all
     * @return number of levels actually refunded
     */
    public static int refundSkill(ServerPlayer player, Skill skill, int count) {
        int refunded = 0;
        while ((count < 0 || refunded < count) && skill.getState(player) == Skill.State.UNLOCKED) {
            skill.lock(player);
            refunded++;
        }
        return refunded;
    }
}
