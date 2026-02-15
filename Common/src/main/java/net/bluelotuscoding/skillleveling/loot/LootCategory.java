package net.bluelotuscoding.skillleveling.loot;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import java.util.*;
import java.util.function.Predicate;

/**
 * Ported and simplified from Apotheosis.
 * Used to identify the category of an item for applying skill imbuements in
 * loot.
 */
public final class LootCategory {
    private static final Map<String, LootCategory> BY_ID_INTERNAL = new HashMap<>();
    private static final List<LootCategory> VALUES_INTERNAL = new ArrayList<>();

    public static final Map<String, LootCategory> BY_ID = Collections.unmodifiableMap(BY_ID_INTERNAL);
    public static final List<LootCategory> VALUES = Collections.unmodifiableList(VALUES_INTERNAL);

    public static final LootCategory SWORD = register("sword", stack -> stack.getItem() instanceof SwordItem);
    public static final LootCategory BOW = register("bow", stack -> stack.getItem() instanceof BowItem);
    public static final LootCategory CROSSBOW = register("crossbow", stack -> stack.getItem() instanceof CrossbowItem);
    public static final LootCategory PICKAXE = register("pickaxe", stack -> stack.getItem() instanceof PickaxeItem);
    public static final LootCategory SHOVEL = register("shovel", stack -> stack.getItem() instanceof ShovelItem);
    public static final LootCategory AXE = register("axe", stack -> stack.getItem() instanceof AxeItem);
    public static final LootCategory HOE = register("hoe", stack -> stack.getItem() instanceof HoeItem);
    public static final LootCategory HELMET = register("helmet", stack -> getArmorSlot(stack) == EquipmentSlot.HEAD);
    public static final LootCategory CHESTPLATE = register("chestplate",
            stack -> getArmorSlot(stack) == EquipmentSlot.CHEST);
    public static final LootCategory LEGGINGS = register("leggings",
            stack -> getArmorSlot(stack) == EquipmentSlot.LEGS);
    public static final LootCategory BOOTS = register("boots", stack -> getArmorSlot(stack) == EquipmentSlot.FEET);
    public static final LootCategory ARMOR = register("armor", stack -> stack.getItem() instanceof ArmorItem);
    public static final LootCategory SHIELD = register("shield", stack -> stack.getItem() instanceof ShieldItem);
    public static final LootCategory TRIDENT = register("trident", stack -> stack.getItem() instanceof TridentItem);
    public static final LootCategory SKILL_CHARM = register("skill_charm",
            stack -> net.minecraft.registry.Registries.ITEM.getId(stack.getItem())
                    .equals(net.bluelotuscoding.skillleveling.registry.ModItems.SKILL_CHARM_ID)
                    || stack.getItem() instanceof net.bluelotuscoding.skillleveling.item.SkillCharmItem);
    public static final LootCategory NONE = register("none", stack -> false);

    private final String name;
    private final Predicate<ItemStack> validator;

    private LootCategory(String name, Predicate<ItemStack> validator) {
        this.name = name;
        this.validator = validator;
    }

    public String getName() {
        return name;
    }

    public boolean isValid(ItemStack stack) {
        return validator.test(stack);
    }

    @Override
    public String toString() {
        return String.format("LootCategory[%s]", name);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LootCategory cat && cat.name.equals(this.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Determines the loot category for an item.
     */
    public static LootCategory forItem(ItemStack item) {
        if (item.isEmpty())
            return NONE;
        for (LootCategory c : VALUES) {
            if (c != NONE && c.isValid(item))
                return c;
        }
        return NONE;
    }

    public static LootCategory byId(String name) {
        return BY_ID.get(name);
    }

    /**
     * Checks if an item stack matches a category string.
     * Category string can be a predefined ID (e.g. "sword") or a tag (e.g.
     * "#minecraft:swords").
     */
    public static boolean matches(ItemStack stack, String key) {
        if (stack.isEmpty())
            return false;

        if (key.startsWith("#")) {
            try {
                String tagIdentifier = key.substring(1);
                TagKey<Item> tagKey = TagKey.of(RegistryKeys.ITEM, new Identifier(tagIdentifier));
                return stack.isIn(tagKey);
            } catch (Exception e) {
                return false;
            }
        }

        LootCategory cat = byId(key);
        return cat != null && cat.isValid(stack);
    }

    private static LootCategory register(String name, Predicate<ItemStack> validator) {
        LootCategory cat = new LootCategory(name, validator);
        BY_ID_INTERNAL.put(name, cat);
        VALUES_INTERNAL.add(cat);
        return cat;
    }

    private static EquipmentSlot getArmorSlot(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armor) {
            return armor.getSlotType();
        }
        return null;
    }
}
