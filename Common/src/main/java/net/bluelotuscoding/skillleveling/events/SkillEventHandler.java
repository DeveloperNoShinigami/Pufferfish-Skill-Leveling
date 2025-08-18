package net.bluelotuscoding.skillleveling.events;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.puffish.skillsmod.api.Skill;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event handler that integrates with the Skills mod to provide per-level rewards
 */
public class SkillEventHandler {

    // Cache of unlocked skills to track which player triggered the event
    private static final Map<Identifier, Map<String, Set<UUID>>> UNLOCKED_PLAYERS = new ConcurrentHashMap<>();
    
    public static void initialize() {
        // Register event listeners using the correct API
        SkillsAPI.registerSkillUnlockEvent(SkillEventHandler::onSkillUnlock);
        SkillsAPI.registerSkillLockEvent(SkillEventHandler::onSkillLock);
    }
    
    private static void onSkillUnlock(Identifier categoryId, String skillId) {
        var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
        manager.getServer().ifPresent(server -> {
            var category = SkillsAPI.getCategory(categoryId);
            if (category.isEmpty()) {
                return;
            }
            var skill = category.get().getSkill(skillId);
            if (skill.isEmpty()) {
                return;
            }

            var cache = UNLOCKED_PLAYERS
                    .computeIfAbsent(categoryId, id -> new ConcurrentHashMap<>())
                    .computeIfAbsent(skillId, id -> ConcurrentHashMap.newKeySet());

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!cache.contains(player.getUuid()) && skill.get().getState(player) == Skill.State.UNLOCKED) {
                    cache.add(player.getUuid());
                    if (!manager.hasSkillData(player, categoryId, skillId)) {
                        manager.initializeSkillData(player, categoryId, skillId);
                    }
                }
            }
        });
    }

    private static void onSkillLock(Identifier categoryId, String skillId) {
        var manager = SkillLevelingMod.getInstance().getSkillLevelingManager();
        manager.getServer().ifPresent(server -> {
            var category = SkillsAPI.getCategory(categoryId);
            if (category.isEmpty()) {
                return;
            }
            var skill = category.get().getSkill(skillId);
            if (skill.isEmpty()) {
                return;
            }

            var skillMap = UNLOCKED_PLAYERS.get(categoryId);
            if (skillMap == null) {
                return;
            }
            var cache = skillMap.get(skillId);
            if (cache == null) {
                return;
            }

            var iterator = cache.iterator();
            while (iterator.hasNext()) {
                var uuid = iterator.next();
                var player = server.getPlayerManager().getPlayer(uuid);
                if (player == null || skill.get().getState(player) != Skill.State.UNLOCKED) {
                    if (player != null) {
                        manager.clearSkillData(player, categoryId, skillId);
                    }
                    iterator.remove();
                }
            }
        });
    }
}
