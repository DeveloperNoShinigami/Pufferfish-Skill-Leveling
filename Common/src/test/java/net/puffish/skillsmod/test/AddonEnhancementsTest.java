package net.puffish.skillsmod.test;

import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.platform.PlatformDetector;
import net.puffish.skillsmod.api.version.VersionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Unit tests for the enhanced addon components.
 * Validates that all new features work correctly.
 */
public class AddonEnhancementsTest {
	
	private VersionManager versionManager;
	
	@BeforeEach
	public void setUp() {
		versionManager = new VersionManager("test-version-1.0.0");
	}
	
	@Test
	public void testVersionManager() {
		// Test version storage
		assertEquals("test-version-1.0.0", versionManager.getCurrentVersion());
		
		// Test update checking is enabled by default
		assertTrue(versionManager.isUpdateCheckEnabled());
		
		// Test disabling update checking
		versionManager.setUpdateCheckEnabled(false);
		assertFalse(versionManager.isUpdateCheckEnabled());
		
		// Test configuration version validation
		assertEquals(VersionManager.VersionCompatibility.COMPATIBLE, 
			versionManager.validateConfigVersion(3));
		assertEquals(VersionManager.VersionCompatibility.TOO_OLD, 
			versionManager.validateConfigVersion(0));
		assertEquals(VersionManager.VersionCompatibility.TOO_NEW, 
			versionManager.validateConfigVersion(999));
	}
	
	@Test
	public void testPlatformDetection() {
		// Test platform detection
		PlatformDetector.PlatformType platform = PlatformDetector.detectPlatform();
		assertNotNull(platform);
		
		// Test display name
		String displayName = PlatformDetector.getPlatformDisplayName();
		assertNotNull(displayName);
		assertFalse(displayName.isEmpty());
		
		// Test config directory name
		String configDir = PlatformDetector.getConfigDirName();
		assertNotNull(configDir);
		assertTrue(configDir.equals("config") || configDir.equals("serverconfig"));
		
		// Test feature support
		// All platforms should support basic features
		for (PlatformDetector.PlatformFeature feature : PlatformDetector.PlatformFeature.values()) {
			// Just test that the method doesn't throw exceptions
			PlatformDetector.supportsFeature(feature);
		}
	}
	
	@Test
	public void testSkillsAPIConstants() {
		// Test that core constants are properly set
		assertEquals("puffish_skills", SkillsAPI.MOD_ID);
		
		// Test platform detection through API
		PlatformDetector.PlatformType platform = SkillsAPI.getCurrentPlatform();
		assertNotNull(platform);
	}
	
	@Test
	public void testVersionCompatibilityMessages() {
		// Test that all compatibility statuses have messages
		for (VersionManager.VersionCompatibility compatibility : VersionManager.VersionCompatibility.values()) {
			String message = versionManager.getCompatibilityMessage(compatibility);
			assertNotNull(message);
			assertFalse(message.isEmpty());
		}
	}
	
	@Test
	public void testAPIInitialization() {
		// Test that initialization doesn't throw exceptions
		Path testConfigDir = Paths.get("test-config");
		
		assertDoesNotThrow(() -> {
			SkillsAPI.initializeAddonComponents("test-version", testConfigDir);
		});
		
		// Test that components are available after initialization
		assertTrue(SkillsAPI.getVersionManager().isPresent());
		assertTrue(SkillsAPI.getConfigurationManager().isPresent());
		assertTrue(SkillsAPI.getDataManager().isPresent());
	}
	
	@Test
	public void testConfigurationVersionValidation() {
		// Test edge cases for version validation
		VersionManager vm = new VersionManager("test");
		
		// Test minimum version
		assertEquals(VersionManager.VersionCompatibility.TOO_OLD,
			vm.validateConfigVersion(-1));
		
		// Test maximum valid version  
		assertEquals(VersionManager.VersionCompatibility.COMPATIBLE,
			vm.validateConfigVersion(3));
		
		// Test outdated but compatible version
		assertEquals(VersionManager.VersionCompatibility.OUTDATED,
			vm.validateConfigVersion(2));
	}
}