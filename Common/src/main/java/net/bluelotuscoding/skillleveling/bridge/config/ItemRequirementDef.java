package net.bluelotuscoding.skillleveling.bridge.config;

import java.util.List;

public class ItemRequirementDef {
    public List<String> items; // Always a list, populated during loading
    public List<String> blocks;
    public List<String> entities;
    public List<String> dimensions;
    public List<String> structures;
    public List<String> dungeons;

    public String require_class;
    public RequireLevelDef require_level;
    public RequireAttributeDef require_attribute;
    public List<String> require_held;
    public List<String> require_worn;
    public List<String> require_effect;
    public List<String> require_quest;

    // Environmental gating
    public Boolean in_water;
    public TimeRangeDef time_of_day;

    public Boolean tooltip;

    public static class RequireLevelDef {
        public String category;
        public int min;
    }

    public static class RequireAttributeDef {
        public String attribute;
        public double min;
    }

    public static class TimeRangeDef {
        public long min;
        public long max;
    }
}
