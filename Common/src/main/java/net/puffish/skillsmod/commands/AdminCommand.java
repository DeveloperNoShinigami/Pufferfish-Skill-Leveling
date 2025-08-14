package net.puffish.skillsmod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.platform.PlatformDetector;

/**
 * Administrative commands for managing the Pufferfish Skills addon.
 * Provides configuration management, data export/import, and system information.
 */
public class AdminCommand {
	
	public static LiteralArgumentBuilder<ServerCommandSource> create() {
		return CommandManager.literal("skillsadmin")
				.requires(source -> source.hasPermissionLevel(3))
				.then(CommandManager.literal("info")
						.executes(AdminCommand::showInfo)
				)
				.then(CommandManager.literal("reload")
						.executes(AdminCommand::reloadConfig)
				)
				.then(CommandManager.literal("backup")
						.then(CommandManager.literal("config")
								.executes(AdminCommand::backupConfig)
						)
						.then(CommandManager.literal("data")
								.executes(AdminCommand::backupData)
						)
				)
				.then(CommandManager.literal("export")
						.then(CommandManager.literal("all")
								.executes(AdminCommand::exportAllData)
						)
				)
				.then(CommandManager.literal("version")
						.then(CommandManager.literal("check")
								.executes(AdminCommand::checkVersion)
						)
				)
				.then(CommandManager.literal("platform")
						.executes(AdminCommand::showPlatformInfo)
				);
	}
	
	private static int showInfo(CommandContext<ServerCommandSource> context) {
		var source = context.getSource();
		
		source.sendFeedback(() -> Text.literal("=== Pufferfish Skills Addon Information ==="), false);
		source.sendFeedback(() -> Text.literal("Platform: " + PlatformDetector.getPlatformDisplayName()), false);
		source.sendFeedback(() -> Text.literal("Mod ID: " + SkillsAPI.MOD_ID), false);
		
		SkillsAPI.getVersionManager().ifPresentOrElse(
			versionManager -> {
				source.sendFeedback(() -> Text.literal("Version: " + versionManager.getCurrentVersion()), false);
				source.sendFeedback(() -> Text.literal("Update Check: " + (versionManager.isUpdateCheckEnabled() ? "Enabled" : "Disabled")), false);
			},
			() -> source.sendFeedback(() -> Text.literal("Version: Unknown"), false)
		);
		
		SkillsAPI.getConfigurationManager().ifPresentOrElse(
			configManager -> source.sendFeedback(() -> Text.literal("Hot Reload: " + (configManager.isHotReloadEnabled() ? "Enabled" : "Disabled")), false),
			() -> source.sendFeedback(() -> Text.literal("Configuration Manager: Not available"), false)
		);
		
		return 1;
	}
	
	private static int reloadConfig(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var source = context.getSource();
		
		SkillsAPI.getConfigurationManager().ifPresentOrElse(
			configManager -> {
				source.sendFeedback(() -> Text.literal("Reloading configurations..."), true);
				
				configManager.reloadConfigurations(source.getServer()).thenAccept(success -> {
					if (success) {
						source.sendFeedback(() -> Text.literal("Configuration reload completed successfully"), true);
					} else {
						source.sendFeedback(() -> Text.literal("Configuration reload failed"), true);
					}
				});
			},
			() -> source.sendError(Text.literal("Configuration manager not available"))
		);
		
		return 1;
	}
	
	private static int backupConfig(CommandContext<ServerCommandSource> context) {
		var source = context.getSource();
		
		SkillsAPI.getDataManager().ifPresentOrElse(
			dataManager -> {
				source.sendFeedback(() -> Text.literal("Creating configuration backup..."), true);
				
				dataManager.backupConfiguration(dataManager.getDataDirectory().getParent()).thenAccept(result -> {
					if (result.success()) {
						source.sendFeedback(() -> Text.literal("Configuration backup created: " + result.exportFile()), true);
					} else {
						source.sendFeedback(() -> Text.literal("Configuration backup failed: " + result.message()), true);
					}
				});
			},
			() -> source.sendError(Text.literal("Data manager not available"))
		);
		
		return 1;
	}
	
	private static int backupData(CommandContext<ServerCommandSource> context) {
		var source = context.getSource();
		
		SkillsAPI.getDataManager().ifPresentOrElse(
			dataManager -> {
				source.sendFeedback(() -> Text.literal("Creating player data backup..."), true);
				
				dataManager.exportAllPlayerData().thenAccept(result -> {
					if (result.success()) {
						source.sendFeedback(() -> Text.literal("Player data backup created: " + result.exportFile()), true);
					} else {
						source.sendFeedback(() -> Text.literal("Player data backup failed: " + result.message()), true);
					}
				});
			},
			() -> source.sendError(Text.literal("Data manager not available"))
		);
		
		return 1;
	}
	
	private static int exportAllData(CommandContext<ServerCommandSource> context) {
		var source = context.getSource();
		
		SkillsAPI.getDataManager().ifPresentOrElse(
			dataManager -> {
				source.sendFeedback(() -> Text.literal("Exporting all player data..."), true);
				
				dataManager.exportAllPlayerData().thenAccept(result -> {
					if (result.success()) {
						source.sendFeedback(() -> Text.literal("Data export completed: " + result.exportFile()), true);
					} else {
						source.sendFeedback(() -> Text.literal("Data export failed: " + result.message()), true);
					}
				});
			},
			() -> source.sendError(Text.literal("Data manager not available"))
		);
		
		return 1;
	}
	
	private static int checkVersion(CommandContext<ServerCommandSource> context) {
		var source = context.getSource();
		
		SkillsAPI.getVersionManager().ifPresentOrElse(
			versionManager -> {
				source.sendFeedback(() -> Text.literal("Checking for updates..."), false);
				
				versionManager.checkForUpdates().thenAccept(updateAvailable -> {
					if (updateAvailable) {
						var latest = versionManager.getLatestVersion().orElse("unknown");
						source.sendFeedback(() -> Text.literal("Update available! Latest version: " + latest), false);
					} else {
						source.sendFeedback(() -> Text.literal("No updates available. Current version: " + versionManager.getCurrentVersion()), false);
					}
				});
			},
			() -> source.sendError(Text.literal("Version manager not available"))
		);
		
		return 1;
	}
	
	private static int showPlatformInfo(CommandContext<ServerCommandSource> context) {
		var source = context.getSource();
		
		source.sendFeedback(() -> Text.literal("=== Platform Information ==="), false);
		source.sendFeedback(() -> Text.literal("Platform: " + PlatformDetector.getPlatformDisplayName()), false);
		source.sendFeedback(() -> Text.literal("Platform Type: " + PlatformDetector.detectPlatform()), false);
		source.sendFeedback(() -> Text.literal("Config Directory: " + PlatformDetector.getConfigDirName()), false);
		
		source.sendFeedback(() -> Text.literal("Supported Features:"), false);
		for (var feature : PlatformDetector.PlatformFeature.values()) {
			boolean supported = PlatformDetector.supportsFeature(feature);
			source.sendFeedback(() -> Text.literal("  " + feature + ": " + (supported ? "Yes" : "No")), false);
		}
		
		return 1;
	}
}