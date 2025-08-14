package com.developernoshingami.pufferfish.skillleveling.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import com.developernoshingami.pufferfish.skillleveling.SkillLevelingMod;

/**
 * Commands for managing multi-level skills, extending the core command system
 */
public class SkillLevelingCommand {
    
    /**
     * Create the command structure for skill leveling
     */
    public static LiteralArgumentBuilder<ServerCommandSource> create() {
        return CommandManager.literal("level")
                .then(CommandManager.literal("info")
                        .executes(SkillLevelingCommand::showInfo)
                )
                .then(CommandManager.literal("advance")
                        .executes(SkillLevelingCommand::advanceSkill)
                )
                .then(CommandManager.literal("check")
                        .executes(SkillLevelingCommand::checkLevels)
                );
    }
    
    /**
     * Show information about skill leveling
     */
    private static int showInfo(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        
        try {
            var player = source.getPlayerOrThrow();
            
            source.sendMessage(Text.literal("=== Skill Leveling Info ==="));
            source.sendMessage(Text.literal("Multi-level skill progression is active!"));
            source.sendMessage(Text.literal("Use /skillleveling check to see your skill levels"));
            source.sendMessage(Text.literal("Use /skillleveling advance to advance skill levels"));
            
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("This command can only be used by players"));
            return 0;
        }
    }
    
    /**
     * Advance a skill level (simplified version - would need more parameters in full implementation)
     */
    private static int advanceSkill(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        
        try {
            var player = source.getPlayerOrThrow();
            
            // This is a simplified version - in a full implementation, this would
            // take category and skill parameters
            source.sendMessage(Text.literal("Skill advancement requires specific skill parameters"));
            source.sendMessage(Text.literal("Use the core skills GUI to interact with your skills"));
            
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("This command can only be used by players"));
            return 0;
        }
    }
    
    /**
     * Check current skill levels
     */
    private static int checkLevels(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        
        try {
            var player = source.getPlayerOrThrow();
            
            source.sendMessage(Text.literal("=== Your Skill Levels ==="));
            
            // Get all categories and their skill levels
            // This would iterate through all categories and skills to show levels
            source.sendMessage(Text.literal("Skill level checking is available through the core skills GUI"));
            source.sendMessage(Text.literal("Enhanced with multi-level progression!"));
            
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("This command can only be used by players"));
            return 0;
        }
    }
}