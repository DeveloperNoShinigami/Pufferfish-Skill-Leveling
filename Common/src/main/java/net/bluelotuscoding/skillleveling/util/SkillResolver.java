package net.bluelotuscoding.skillleveling.util;

import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.LeveledConfig;
import java.util.Map;
import java.util.Optional;

public final class SkillResolver {

    public record Resolved(String fullId, String categoryId, LeveledConfig config) {
    }

    /**
     * Attempts to resolve a skill ID (e.g. "sharpness" or "combat:sharpness")
     * from the loaded LeveledConfigStorage.
     * 
     * @param id The skill ID to resolve
     * @return Optional containing the resolved skill, or empty if not found
     */
    public static Optional<Resolved> resolve(String id) {
        if (id == null || id.isEmpty())
            return Optional.empty();

        // 1. Direct lookup (e.g. "minecraft:sharpness" or "sharpness" if registered as
        // such)
        if (LeveledConfigStorage.has(id)) {
            LeveledConfig config = LeveledConfigStorage.get(id);
            if (config != null) {
                return Optional.of(new Resolved(id, config.categoryId, config));
            }
        }

        // 2. Resolve by path (e.g. "sharpness" -> "some_namespace:sharpness")
        // Iterate over all entries since keys might include namespace
        for (Map.Entry<String, LeveledConfig> entry : LeveledConfigStorage.getAllEntries().entrySet()) {
            String fullKey = entry.getKey();

            // Try separating by colon
            int colonIndex = fullKey.indexOf(':');
            String path = (colonIndex >= 0) ? fullKey.substring(colonIndex + 1) : fullKey;

            if (path.equals(id)) {
                // Should we check category ambiguity? Assume first match for now as per plan
                LeveledConfig config = entry.getValue();
                return Optional.of(new Resolved(fullKey, config.categoryId, config));
            }
        }

        return Optional.empty();
    }
}
