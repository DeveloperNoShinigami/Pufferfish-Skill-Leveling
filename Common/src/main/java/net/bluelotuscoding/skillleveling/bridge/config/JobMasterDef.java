package net.bluelotuscoding.skillleveling.bridge.config;

import java.util.List;

public class JobMasterDef {
    public String id; // Unique ID for this Job Master
    public String marker_block; // Registry ID of the block that triggers spawning
    public String texture; // ResourceLocation for the NPC texture
    public String dialogue_key; // Translation key for the quest/dialogue
    public String name_key; // Translation key for the NPC name
    public Gear equipment; // Equipment for the NPC

    public static class Gear {
        public String mainhand;
        public String offhand;
        public String helmet;
        public String chestplate;
        public String leggings;
        public String boots;
    }
}
