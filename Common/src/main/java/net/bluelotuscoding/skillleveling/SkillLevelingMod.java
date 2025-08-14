package net.bluelotuscoding.skillleveling;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.server.event.ServerEventReceiver;
import net.puffish.skillsmod.server.network.ServerPacketSender;
import net.puffish.skillsmod.server.setup.ServerPlatform;
import net.puffish.skillsmod.server.setup.ServerRegistrar;
import net.bluelotuscoding.skillleveling.manager.SkillLevelingManager;
import net.bluelotuscoding.skillleveling.commands.SkillLevelingCommand;
import net.bluelotuscoding.skillleveling.rewards.PerLevelReward;

import java.nio.file.Path;

/**
 * Main addon class that integrates with Pufferfish Skills to provide multi-level skill progression.
 */
public class SkillLevelingMod {
    public static final String MOD_ID = "puffish_skill_leveling";
    
    private static SkillLevelingMod instance;
    
    private final SkillLevelingManager skillLevelingManager;
    
    private SkillLevelingMod() {
        this.skillLevelingManager = new SkillLevelingManager();
    }
    
    public static void setup(
            Path configDir,
            ServerRegistrar registrar,
            ServerEventReceiver eventReceiver,
            ServerPacketSender packetSender,
            ServerPlatform platform
    ) {
        instance = new SkillLevelingMod();
        
        // Register our addon with the core mod
        instance.initialize(configDir, registrar, eventReceiver, packetSender, platform);
    }
    
    private void initialize(
            Path configDir,
            ServerRegistrar registrar,
            ServerEventReceiver eventReceiver,
            ServerPacketSender packetSender,
            ServerPlatform platform
    ) {
        // Register event listeners to extend core functionality
        eventReceiver.registerListener(new SkillLevelingEventListener());
        
        // Register custom rewards that provide per-level functionality
        SkillsAPI.registerReward(createIdentifier("per_level"), PerLevelReward::create);
        
        // Register commands for managing multi-level skills
        // Commands will be registered through the event listener
    }
    
    public static SkillLevelingMod getInstance() {
        return instance;
    }
    
    public SkillLevelingManager getSkillLevelingManager() {
        return skillLevelingManager;
    }
    
    public static Identifier createIdentifier(String path) {
        return new Identifier(MOD_ID, path);
    }
    
    /**
     * Check if a player has unlocked a specific level of a skill
     */
    public boolean hasSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId, int level) {
        return skillLevelingManager.hasSkillLevel(player, categoryId, skillId, level);
    }
    
    /**
     * Get the current level of a skill for a player
     */
    public int getSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        return skillLevelingManager.getSkillLevel(player, categoryId, skillId);
    }
    
    /**
     * Advance a skill to the next level for a player
     */
    public void advanceSkillLevel(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        skillLevelingManager.advanceSkillLevel(player, categoryId, skillId);
    }
}