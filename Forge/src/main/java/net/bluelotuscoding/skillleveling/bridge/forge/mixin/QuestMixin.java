package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

/**
 * RESERVED FOR FUTURE USE
 * 
 * This mixin is a placeholder for potential quest-specific handling.
 * 
 * Currently not needed because:
 * - All exp capture is handled by PlayerLevelDataMixin (catches all addXp calls)
 * - Quest exp is automatically synced like any other exp source
 * 
 * Future use cases:
 * - Apply quest completion bonuses (XP multiplier, skill rewards, etc)
 * - Log quest completions for analytics
 * - Apply quest-only bonus skew
 * - Prevent certain quests from granting exp to specific categories
 * 
 * To activate: Add "QuestMixin" to puffish_skill_leveling_bridge.mixins.json "server" section
 */
public class QuestMixin {
}