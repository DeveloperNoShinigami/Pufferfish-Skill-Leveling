package net.puffish.skillsmod.api.platform;

/**
 * Detects the current mod loader platform and provides platform-specific capabilities.
 * This allows the addon to adapt its behavior based on the hosting environment.
 */
public class PlatformDetector {
	
	private static PlatformType detectedPlatform = null;
	
	/**
	 * Detects the current platform by checking for platform-specific classes.
	 */
	public static PlatformType detectPlatform() {
		if (detectedPlatform != null) {
			return detectedPlatform;
		}
		
		// Check for Fabric
		try {
			Class.forName("net.fabricmc.api.ModInitializer");
			detectedPlatform = PlatformType.FABRIC;
			return detectedPlatform;
		} catch (ClassNotFoundException ignored) {
			// Not Fabric
		}
		
		// Check for Forge
		try {
			Class.forName("net.minecraftforge.fml.common.Mod");
			detectedPlatform = PlatformType.FORGE;
			return detectedPlatform;
		} catch (ClassNotFoundException ignored) {
			// Not Forge
		}
		
		// Check for NeoForge
		try {
			Class.forName("net.neoforged.fml.common.Mod");
			detectedPlatform = PlatformType.NEOFORGE;
			return detectedPlatform;
		} catch (ClassNotFoundException ignored) {
			// Not NeoForge
		}
		
		// Check for Quilt
		try {
			Class.forName("org.quiltmc.loader.api.ModContainer");
			detectedPlatform = PlatformType.QUILT;
			return detectedPlatform;
		} catch (ClassNotFoundException ignored) {
			// Not Quilt
		}
		
		detectedPlatform = PlatformType.UNKNOWN;
		return detectedPlatform;
	}
	
	/**
	 * Checks if the platform supports specific features.
	 */
	public static boolean supportsFeature(PlatformFeature feature) {
		PlatformType platform = detectPlatform();
		
		return switch (feature) {
			case DATA_GENERATION -> platform == PlatformType.FABRIC || platform == PlatformType.FORGE || platform == PlatformType.NEOFORGE;
			case MIXINS -> platform != PlatformType.UNKNOWN;
			case CLIENT_EVENTS -> platform != PlatformType.UNKNOWN;
			case SERVER_EVENTS -> platform != PlatformType.UNKNOWN;
			case RESOURCE_PACKS -> platform != PlatformType.UNKNOWN;
			case DATA_PACKS -> platform != PlatformType.UNKNOWN;
		};
	}
	
	/**
	 * Gets platform-specific configuration directory name.
	 */
	public static String getConfigDirName() {
		return switch (detectPlatform()) {
			case FABRIC, QUILT -> "config";
			case FORGE, NEOFORGE -> "serverconfig";
			case UNKNOWN -> "config";
		};
	}
	
	/**
	 * Gets the display name for the current platform.
	 */
	public static String getPlatformDisplayName() {
		return switch (detectPlatform()) {
			case FABRIC -> "Fabric";
			case FORGE -> "Forge";
			case NEOFORGE -> "NeoForge";
			case QUILT -> "Quilt";
			case UNKNOWN -> "Unknown";
		};
	}
	
	public enum PlatformType {
		FABRIC,
		FORGE,
		NEOFORGE,
		QUILT,
		UNKNOWN
	}
	
	public enum PlatformFeature {
		DATA_GENERATION,
		MIXINS,
		CLIENT_EVENTS,
		SERVER_EVENTS,
		RESOURCE_PACKS,
		DATA_PACKS
	}
}