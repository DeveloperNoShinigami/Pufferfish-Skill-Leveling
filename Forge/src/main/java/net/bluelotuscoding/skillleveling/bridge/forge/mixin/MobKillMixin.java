package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

/**
 * RESERVED FOR FUTURE USE
 * 
 * This mixin is a placeholder for potential mob kill-specific handling.
 * 
 * Currently not needed because:
 * - All exp capture is handled by PlayerLevelDataMixin
 * - All level/progression is handled by Pufferfish
 * 
 * Future use cases:
 * - Apply Pufferfish's kill_entity multiplier to Epic Class's calculated exp
 * - Log mob kill events for analytics
 * - Apply special mob-kill-only bonuses
 * 
 * To activate: Add "MobKillMixin" to puffish_skill_leveling_bridge.mixins.json "server" section
 */
public class MobKillMixin {
}
