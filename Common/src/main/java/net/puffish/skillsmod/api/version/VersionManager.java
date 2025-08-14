package net.puffish.skillsmod.api.version;

import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.util.PrefixedLogger;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Manages version checking and update notifications for the Pufferfish Skills addon.
 * Provides capabilities for detecting updates and validating compatibility.
 */
public class VersionManager {
	private static final PrefixedLogger LOGGER = new PrefixedLogger(SkillsAPI.MOD_ID + ".version");
	
	private final String currentVersion;
	private Optional<String> latestVersion = Optional.empty();
	private boolean updateCheckEnabled = true;
	
	public VersionManager(String currentVersion) {
		this.currentVersion = currentVersion;
	}
	
	/**
	 * Gets the current mod version.
	 */
	public String getCurrentVersion() {
		return currentVersion;
	}
	
	/**
	 * Gets the latest available version if known.
	 */
	public Optional<String> getLatestVersion() {
		return latestVersion;
	}
	
	/**
	 * Enables or disables update checking.
	 */
	public void setUpdateCheckEnabled(boolean enabled) {
		this.updateCheckEnabled = enabled;
	}
	
	/**
	 * Checks if update checking is enabled.
	 */
	public boolean isUpdateCheckEnabled() {
		return updateCheckEnabled;
	}
	
	/**
	 * Checks if an update is available.
	 */
	public boolean isUpdateAvailable() {
		return latestVersion.map(latest -> !latest.equals(currentVersion)).orElse(false);
	}
	
	/**
	 * Asynchronously checks for updates from the configured source.
	 * This is a placeholder implementation that would connect to a update server.
	 */
	public CompletableFuture<Boolean> checkForUpdates() {
		if (!updateCheckEnabled) {
			return CompletableFuture.completedFuture(false);
		}
		
		return CompletableFuture.supplyAsync(() -> {
			try {
				LOGGER.info("Checking for updates...");
				// TODO: Implement actual update checking logic
				// This would typically connect to GitHub releases API or a mod repository
				
				// For now, simulate no updates available
				latestVersion = Optional.of(currentVersion);
				
				LOGGER.info("Update check completed. Current version: {}, Latest: {}", 
					currentVersion, latestVersion.orElse("unknown"));
				
				return isUpdateAvailable();
			} catch (Exception e) {
				LOGGER.warn("Failed to check for updates: {}", e.getMessage());
				return false;
			}
		});
	}
	
	/**
	 * Validates if a configuration version is compatible with the current mod version.
	 */
	public VersionCompatibility validateConfigVersion(int configVersion) {
		if (configVersion < SkillsMod.MIN_CONFIG_VERSION) {
			return VersionCompatibility.TOO_OLD;
		} else if (configVersion > SkillsMod.MAX_CONFIG_VERSION) {
			return VersionCompatibility.TOO_NEW;
		} else if (configVersion < SkillsMod.MAX_CONFIG_VERSION) {
			return VersionCompatibility.OUTDATED;
		} else {
			return VersionCompatibility.COMPATIBLE;
		}
	}
	
	/**
	 * Gets a user-friendly message for version compatibility status.
	 */
	public String getCompatibilityMessage(VersionCompatibility compatibility) {
		return switch (compatibility) {
			case COMPATIBLE -> "Configuration version is up to date.";
			case OUTDATED -> "Configuration uses an older version. Consider updating to version " + SkillsMod.MAX_CONFIG_VERSION + ".";
			case TOO_OLD -> "Configuration is too old and incompatible. Please update to version " + SkillsMod.MAX_CONFIG_VERSION + ".";
			case TOO_NEW -> "Configuration is for a newer version of the mod. Please update the mod.";
		};
	}
	
	public enum VersionCompatibility {
		COMPATIBLE,
		OUTDATED,
		TOO_OLD,
		TOO_NEW
	}
}