package net.bluelotuscoding.puffishskillleveling.client.config;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.puffishskillleveling.client.config.ExtendedClientBackgroundConfig;
import net.puffish.skillsmod.client.config.ClientIconConfig;
import net.puffish.skillsmod.client.config.colors.ClientColorsConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillConnectionConfig;
import net.bluelotuscoding.puffishskillleveling.client.config.skill.ExtendedClientSkillDefinitionConfig;
import net.puffish.skillsmod.util.Bounds2i;
import org.joml.Vector2i;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

/**
 * Variant of {@link net.puffish.skillsmod.client.config.ClientCategoryConfig}
 * that stores {@link ExtendedClientSkillDefinitionConfig} entries. This is used by
 * the client addon to retain additional data sent from the server while reusing
 * the rest of the base configuration logic.
 */
public record ExtendedClientCategoryConfig(
                Identifier id,
                Text title,
                ClientIconConfig icon,
                ExtendedClientBackgroundConfig background,
                ClientColorsConfig colors,
                boolean exclusiveRoot,
                int spentPointsLimit,
                int levelLimit,
                Map<String, ExtendedClientSkillDefinitionConfig> definitions,
                Map<String, ClientSkillConfig> skills,
                Collection<ClientSkillConnectionConfig> normalConnections,
                Collection<ClientSkillConnectionConfig> exclusiveConnections,
                Map<String, Collection<ClientSkillConnectionConfig>> skillNormalConnections,
                Map<String, Collection<ClientSkillConnectionConfig>> skillExclusiveConnections,
                Map<String, Collection<String>> skillNormalNeighbors,
                Map<String, Collection<String>> skillExclusiveNeighbors,
                Map<String, Collection<String>> skillNormalNeighborsReversed,
                Map<String, Collection<String>> skillExclusiveNeighborsReversed
) {
        public ExtendedClientCategoryConfig(
                        Identifier id,
                        Text title,
                        ClientIconConfig icon,
                        ExtendedClientBackgroundConfig background,
                        ClientColorsConfig colors,
                        boolean exclusiveRoot,
                        int spentPointsLimit,
                        int levelLimit,
                        Map<String, ExtendedClientSkillDefinitionConfig> definitions,
                        Map<String, ClientSkillConfig> skills,
                        Collection<ClientSkillConnectionConfig> normalConnections,
                        Collection<ClientSkillConnectionConfig> exclusiveConnections
        ) {
                this(
                                id,
                                title,
                                icon,
                                background,
                                colors,
                                exclusiveRoot,
                                spentPointsLimit,
                                levelLimit,
                                definitions,
                                skills,
                                normalConnections,
                                exclusiveConnections,
                                new HashMap<>(),
                                new HashMap<>(),
                                new HashMap<>(),
                                new HashMap<>(),
                                new HashMap<>(),
                                new HashMap<>()
                );

                for (var connection : normalConnections) {
                        var a = connection.skillAId();
                        var b = connection.skillBId();

                        skillNormalNeighbors.computeIfAbsent(a, key -> new HashSet<>()).add(b);
                        skillNormalNeighborsReversed.computeIfAbsent(b, key -> new HashSet<>()).add(a);
                        if (connection.bidirectional()) {
                                skillNormalNeighbors.computeIfAbsent(b, key -> new HashSet<>()).add(a);
                                skillNormalNeighborsReversed.computeIfAbsent(a, key -> new HashSet<>()).add(b);
                        }

                        skillNormalConnections.computeIfAbsent(a, key -> new HashSet<>()).add(connection);
                        skillNormalConnections.computeIfAbsent(b, key -> new HashSet<>()).add(connection);
                }

                for (var connection : exclusiveConnections) {
                        var a = connection.skillAId();
                        var b = connection.skillBId();

                        skillExclusiveNeighbors.computeIfAbsent(a, key -> new HashSet<>()).add(b);
                        skillExclusiveNeighborsReversed.computeIfAbsent(b, key -> new HashSet<>()).add(a);
                        if (connection.bidirectional()) {
                                skillExclusiveNeighbors.computeIfAbsent(b, key -> new HashSet<>()).add(a);
                                skillExclusiveNeighborsReversed.computeIfAbsent(a, key -> new HashSet<>()).add(b);
                        }

                        this.skillExclusiveConnections.computeIfAbsent(a, key -> new HashSet<>()).add(connection);
                        this.skillExclusiveConnections.computeIfAbsent(b, key -> new HashSet<>()).add(connection);
                }
        }

        public Bounds2i getBounds() {
                var bounds = Bounds2i.zero();
                for (var skill : skills.values()) {
                        bounds.extend(new Vector2i(skill.x(), skill.y()));
                }
                return bounds;
        }

        public Optional<ExtendedClientSkillDefinitionConfig> getDefinitionById(String id) {
                return Optional.ofNullable(definitions.get(id));
        }
}
