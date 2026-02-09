package net.bluelotuscoding.skillleveling.data;

import java.util.ArrayList;
import java.util.List;

public class GlobalLootConfig {
    public List<EntityDropGroup> entityDrops = new ArrayList<>();
    public List<ChestInjectionGroup> chestInjections = new ArrayList<>();

    public static class EntityDropGroup {
        public List<String> entityTypes = new ArrayList<>();
        public List<LootEntry> entries = new ArrayList<>();
    }

    public static class ChestInjectionGroup {
        public List<String> containers = new ArrayList<>();
        public List<LootEntry> entries = new ArrayList<>();
    }

    public static class LootEntry {
        public String type = "item"; // "item" or "skill_tome"
        public String item = ""; // registry id of the item
        public float chance = 0.0f; // for entity drops
        public int weight = 1; // for chest injections
        public int minLevel = 1;
        public int maxLevel = 1;
        public String nbt = null; // optional stringified NBT
    }
}
