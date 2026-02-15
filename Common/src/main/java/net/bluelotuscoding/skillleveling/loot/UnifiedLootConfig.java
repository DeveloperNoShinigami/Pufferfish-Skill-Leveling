package net.bluelotuscoding.skillleveling.loot;

import java.util.List;
import java.util.Optional;

/**
 * Unified configuration structure for loot injection and imbuing.
 */
public final class UnifiedLootConfig {

    public record Rolls(int min, int max) {
    }

    public record SkillPoolEntry(String skill, int weight) {
    }

    public record LootEntry(
            String type,
            String name,
            float chance,
            int weight,
            int minLevel,
            int maxLevel,
            Optional<String> nbt,
            Optional<String> skill,
            List<SkillPoolEntry> skills) {
    }

    public record LootGroup(
            List<String> targets,
            float chance,
            Rolls rolls,
            List<LootEntry> entries) {
    }
}
