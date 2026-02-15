package net.bluelotuscoding.skillleveling.loot;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Utility for matching loot table IDs or Item tags.
 */
public final class LootMatchingUtil {

    /**
     * Checks if a target ID (e.g. "minecraft:entities/zombie") matches any of the
     * strings in the target list.
     * Supports direct ID matches and simple prefix matches if needed.
     */
    public static boolean matchesTarget(String targetId, List<String> targets) {
        if (targetId == null || targets == null) {
            return false;
        }

        if (targets.contains("any")) {
            return true;
        }

        Identifier actualId = Identifier.tryParse(targetId);
        if (actualId == null)
            return false;

        for (String target : targets) {
            boolean match = false;
            if (target.equals(targetId)) {
                match = true;
            } else if (!target.contains(":")) {
                // If no namespace provided, match based on path
                if (actualId.getPath().equals(target) || actualId.getPath().endsWith("/" + target)) {
                    match = true;
                }
            } else {
                // If namespace provided, parse and compare
                Identifier targetIdentifier = Identifier.tryParse(target);
                if (targetIdentifier != null) {
                    if (targetIdentifier.getNamespace().equals(actualId.getNamespace()) &&
                            (targetIdentifier.getPath().equals(actualId.getPath()) ||
                                    actualId.getPath().endsWith("/" + targetIdentifier.getPath()))) {
                        match = true;
                    }
                }
            }

            if (match) {
                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                        .debug("[LootMatchingUtil] Match found: " + targetId + " matches target " + target);
                return true;
            }
        }
        return false;
    }

    /**
     * Helper to infer the Loot Table ID from the entity in the context.
     * Useful when the direct loot table ID is missing.
     */
    public static Identifier inferLootTableId(net.minecraft.loot.context.LootContext context) {
        var entity = context.get(net.minecraft.loot.context.LootContextParameters.THIS_ENTITY);
        if (entity != null) {
            Identifier entityId = net.minecraft.registry.Registries.ENTITY_TYPE.getId(entity.getType());
            Identifier inferred = new Identifier(entityId.getNamespace(), "entities/" + entityId.getPath());
            net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                    .debug("[LootMatchingUtil] Inferred ID for " + entityId + " -> " + inferred);
            return inferred;
        }
        net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                .debug("[LootMatchingUtil] No entity found in context to infer ID from.");
        return null;
    }

    /**
     * Checks if an item stack matches a category string.
     * Supports tags (starting with #) or LootCategory IDs.
     */
    public static boolean matchesCategory(ItemStack stack, String key) {
        if (stack.isEmpty() || key == null) {
            return false;
        }

        if (key.startsWith("#")) {
            try {
                String tagIdentifier = key.substring(1);
                TagKey<Item> tagKey = TagKey.of(RegistryKeys.ITEM, new Identifier(tagIdentifier));
                return stack.isIn(tagKey);
            } catch (Exception e) {
                return false;
            }
        }

        LootCategory cat = LootCategory.byId(key);
        return cat != null && cat.isValid(stack);
    }
}
