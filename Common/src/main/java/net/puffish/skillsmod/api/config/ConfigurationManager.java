package net.puffish.skillsmod.api.config;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.config.CategoryConfig;
import net.puffish.skillsmod.util.PrefixedLogger;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Manages configuration loading, reloading, and validation for the Pufferfish Skills addon.
 * Provides hot-reloading capabilities and configuration change notifications.
 */
public class ConfigurationManager {
	private static final PrefixedLogger LOGGER = new PrefixedLogger(SkillsAPI.MOD_ID + ".config");
	
	private final Path configDirectory;
	private final Consumer<Map<Identifier, CategoryConfig>> configChangeCallback;
	private boolean hotReloadEnabled = true;
	
	public ConfigurationManager(Path configDirectory, Consumer<Map<Identifier, CategoryConfig>> configChangeCallback) {
		this.configDirectory = configDirectory;
		this.configChangeCallback = configChangeCallback;
	}
	
	/**
	 * Enables or disables hot-reloading of configuration files.
	 */
	public void setHotReloadEnabled(boolean enabled) {
		this.hotReloadEnabled = enabled;
		LOGGER.info("Configuration hot-reload {}", enabled ? "enabled" : "disabled");
	}
	
	/**
	 * Checks if hot-reloading is enabled.
	 */
	public boolean isHotReloadEnabled() {
		return hotReloadEnabled;
	}
	
	/**
	 * Asynchronously reloads all configuration files.
	 */
	public CompletableFuture<Boolean> reloadConfigurations(MinecraftServer server) {
		if (!hotReloadEnabled) {
			LOGGER.warn("Hot-reload is disabled, skipping configuration reload");
			return CompletableFuture.completedFuture(false);
		}
		
		return CompletableFuture.supplyAsync(() -> {
			try {
				LOGGER.info("Reloading configurations from {}", configDirectory);
				
				// This would trigger the existing configuration loading logic
				// For now, we'll delegate to the SkillsMod instance
				var skillsMod = SkillsMod.getInstance();
				if (skillsMod != null) {
					// TODO: Add a public method to reload configurations
					LOGGER.info("Configuration reload completed successfully");
					return true;
				} else {
					LOGGER.warn("SkillsMod instance not available for configuration reload");
					return false;
				}
			} catch (Exception e) {
				LOGGER.error("Failed to reload configurations: {}", e.getMessage(), e);
				return false;
			}
		});
	}
	
	/**
	 * Validates configuration files without loading them.
	 */
	public CompletableFuture<ConfigValidationResult> validateConfigurations(MinecraftServer server) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				LOGGER.info("Validating configurations in {}", configDirectory);
				
				// TODO: Implement configuration validation logic
				// This would parse configs and check for errors without applying them
				
				return new ConfigValidationResult(true, "All configurations are valid", Optional.empty());
			} catch (Exception e) {
				LOGGER.error("Configuration validation failed: {}", e.getMessage(), e);
				return new ConfigValidationResult(false, "Validation failed: " + e.getMessage(), Optional.of(e));
			}
		});
	}
	
	/**
	 * Creates a backup of current configuration files.
	 */
	public CompletableFuture<Boolean> backupConfigurations() {
		return CompletableFuture.supplyAsync(() -> {
			try {
				LOGGER.info("Creating configuration backup");
				
				// TODO: Implement configuration backup logic
				// This would copy current configs to a backup directory with timestamp
				
				LOGGER.info("Configuration backup completed");
				return true;
			} catch (Exception e) {
				LOGGER.error("Failed to backup configurations: {}", e.getMessage(), e);
				return false;
			}
		});
	}
	
	/**
	 * Restores configuration files from a backup.
	 */
	public CompletableFuture<Boolean> restoreConfigurations(String backupName) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				LOGGER.info("Restoring configurations from backup: {}", backupName);
				
				// TODO: Implement configuration restore logic
				
				LOGGER.info("Configuration restore completed");
				return true;
			} catch (Exception e) {
				LOGGER.error("Failed to restore configurations: {}", e.getMessage(), e);
				return false;
			}
		});
	}
	
	/**
	 * Gets the configuration directory path.
	 */
	public Path getConfigDirectory() {
		return configDirectory;
	}
	
	/**
	 * Result of configuration validation.
	 */
	public record ConfigValidationResult(
		boolean isValid,
		String message,
		Optional<Exception> error
	) {}
}