package net.bluelotuscoding.skillleveling.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LootImbueConfig {
    public DistanceScaling distanceScaling = new DistanceScaling();
    public Map<String, DimensionOverride> dimensionOverrides = new HashMap<>();
    public List<String> itemBlacklist = new ArrayList<>();
    public List<String> itemWhitelist = new ArrayList<>(); // New: Explicit allowed items/tags
    public List<String> lootTableWhitelist = new ArrayList<>(); // New: Allowed loot tables
    public List<ExclusionGroup> exclusionGroups = new ArrayList<>();

    public List<ImbueEntry> global = new ArrayList<>();
    public Map<String, List<ImbueEntry>> categories = new HashMap<>();
    public Map<String, CategorySettings> categorySettings = new HashMap<>(); // New: Per-category overrides

    public static class ImbueEntry {
        public String skill;
        public int weight;

        public ImbueEntry(String skill, int weight) {
            this.skill = skill;
            this.weight = weight;
        }
    }

    public static class DistanceScaling {
        public boolean enabled = true;
        public int[] origin = { 0, 0 };
        public List<Bracket> brackets = new ArrayList<>();
    }

    public static class Bracket {
        public int distance;
        public int maxLevel;
        public double chanceMult;
    }

    public static class DimensionOverride {
        public double imbueChance = 0.0;
        public int maxSkills = 0;
        public int minLevel = 1;
        public int maxLevel = 1;
    }

    public static class ExclusionGroup {
        public List<String> types = new ArrayList<>();
    }

    // New class for per-category settings
    public static class CategorySettings {
        public double imbueChance = -1.0; // -1 means use global/dimension default
        public int minLevel = -1;
        public int maxLevel = -1;
        public int maxSkills = -1;
    }
}
