package net.bluelotuscoding.skillleveling.bridge.config;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class EpicClassDef {
    public String class_name;
    public String class_parent;
    public String display_name_key;
    public String lore_key;

    public String skill_category_id;
    public String epic_class_proxy;
    /** When true, this class shows the mana (sorcerer) tab in the ClassBookScreen. */
    public boolean is_sorcerer_type = false;

    /** ECM character level required to advance INTO this class (i.e. on the child class). */
    public int required_level = 0;

    // --- New Class Book Fields ---
    public String book_lore;
    public String class_weapon_type;
    public String class_weapon_icon;
    public List<String> class_weapon_items;

    public Map<String, AttributeDef> attributes = new HashMap<>();

    /** ECM stat points granted per Pufferfish level. 0 = use BridgeConfig global. */
    public int stat_points_per_level = 0;

    // --- Branding & Text ---
    public String display_name;
    public String description;

    // --- UI Fields for Class Select Screen ---
    public String gui_title;
    public String gui_description;
    public String preview_animation;

    public String preview_armor_base;
    public String preview_mainhand_item;
    public String preview_offhand_item;

    public List<String> gui_notes;
    public List<String> starting_items;

    public List<StatDef> gui_stats;
    public List<PassiveUIDef> gui_passives;

    public static class AttributeDef {
        public String attribute;
        public String operation;
        public double value;
        public String command = null;
    }

    public static class StatDef {
        public String label_key;
        public String icon;
        /** "hearts" (default) or "number" — controls how the stat is displayed */
        public String stat_type;
        /**
         * For "hearts" mode: how many icons to draw. For "number" mode: the raw value
         * shown.
         */
        public int count;
        /**
         * Optional unit label appended when stat_type is "number", e.g. "HP", "DMG",
         * "Mana"
         */
        public String unit;
    }

    public static class PassiveUIDef {
        public String name_key;
        public String desc_key;
        public String icon;
        /**
         * When set, the screen will auto-fetch the passive's title and description
         * from the corresponding Pufferfish skill definition instead of using the
         * name_key / desc_key translation key fields.
         * Format: just the skill ID path (e.g. "skeleton_mastery") — the category
         * is resolved from skill_category_id on this class def.
         */
        public String pufferfish_skill_id;
        /**
         * The Pufferfish level required to unlock this passive. 
         * Overrides the global bridge mapping if set.
         */
        public int level;
    }
}
