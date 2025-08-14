package net.puffish.skillsmod.test;

import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.platform.PlatformDetector;
import net.puffish.skillsmod.api.version.VersionManager;
import net.puffish.skillsmod.util.PrefixedLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Automated testing framework for validating addon functionality.
 * Provides integration tests for core features and components.
 */
public class AddonTestFramework {
	private static final PrefixedLogger LOGGER = new PrefixedLogger(SkillsAPI.MOD_ID + ".test");
	
	private final List<TestCase> testCases = new ArrayList<>();
	
	public AddonTestFramework() {
		registerTestCases();
	}
	
	private void registerTestCases() {
		testCases.add(new PlatformDetectionTest());
		testCases.add(new VersionManagerTest());
		testCases.add(new APIInitializationTest());
		testCases.add(new ConfigurationValidationTest());
	}
	
	/**
	 * Runs all registered test cases.
	 */
	public CompletableFuture<TestResults> runAllTests() {
		return CompletableFuture.supplyAsync(() -> {
			LOGGER.info("Starting addon test suite...");
			
			List<TestResult> results = new ArrayList<>();
			int passed = 0;
			int failed = 0;
			
			for (TestCase testCase : testCases) {
				try {
					LOGGER.info("Running test: {}", testCase.getName());
					TestResult result = testCase.execute();
					results.add(result);
					
					if (result.passed()) {
						passed++;
						LOGGER.info("Test passed: {}", testCase.getName());
					} else {
						failed++;
						LOGGER.warn("Test failed: {} - {}", testCase.getName(), result.message());
					}
				} catch (Exception e) {
					failed++;
					results.add(new TestResult(testCase.getName(), false, "Test threw exception: " + e.getMessage()));
					LOGGER.error("Test threw exception: {}", testCase.getName(), e);
				}
			}
			
			LOGGER.info("Test suite completed. Passed: {}, Failed: {}", passed, failed);
			return new TestResults(results, passed, failed);
		});
	}
	
	/**
	 * Base interface for test cases.
	 */
	public interface TestCase {
		String getName();
		TestResult execute();
	}
	
	/**
	 * Result of a single test.
	 */
	public record TestResult(String testName, boolean passed, String message) {}
	
	/**
	 * Results of all tests.
	 */
	public record TestResults(List<TestResult> results, int passed, int failed) {
		public boolean allPassed() {
			return failed == 0;
		}
	}
	
	/**
	 * Test platform detection functionality.
	 */
	private static class PlatformDetectionTest implements TestCase {
		@Override
		public String getName() {
			return "Platform Detection";
		}
		
		@Override
		public TestResult execute() {
			try {
				PlatformDetector.PlatformType platform = PlatformDetector.detectPlatform();
				
				if (platform == PlatformDetector.PlatformType.UNKNOWN) {
					return new TestResult(getName(), false, "Platform detection returned UNKNOWN");
				}
				
				String displayName = PlatformDetector.getPlatformDisplayName();
				if (displayName == null || displayName.isEmpty()) {
					return new TestResult(getName(), false, "Platform display name is null or empty");
				}
				
				return new TestResult(getName(), true, "Platform detected as: " + displayName);
			} catch (Exception e) {
				return new TestResult(getName(), false, "Exception: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Test version manager functionality.
	 */
	private static class VersionManagerTest implements TestCase {
		@Override
		public String getName() {
			return "Version Manager";
		}
		
		@Override
		public TestResult execute() {
			try {
				VersionManager versionManager = new VersionManager("test-version");
				
				if (!versionManager.getCurrentVersion().equals("test-version")) {
					return new TestResult(getName(), false, "Version manager did not store version correctly");
				}
				
				if (!versionManager.isUpdateCheckEnabled()) {
					return new TestResult(getName(), false, "Update check should be enabled by default");
				}
				
				// Test version compatibility
				var compatibility = versionManager.validateConfigVersion(3);
				if (compatibility == null) {
					return new TestResult(getName(), false, "Version compatibility check returned null");
				}
				
				return new TestResult(getName(), true, "Version manager tests passed");
			} catch (Exception e) {
				return new TestResult(getName(), false, "Exception: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Test API initialization.
	 */
	private static class APIInitializationTest implements TestCase {
		@Override
		public String getName() {
			return "API Initialization";
		}
		
		@Override
		public TestResult execute() {
			try {
				// Check if MOD_ID is properly set
				if (SkillsAPI.MOD_ID == null || SkillsAPI.MOD_ID.isEmpty()) {
					return new TestResult(getName(), false, "MOD_ID is null or empty");
				}
				
				// Check platform detection
				PlatformDetector.PlatformType platform = SkillsAPI.getCurrentPlatform();
				if (platform == null) {
					return new TestResult(getName(), false, "Platform detection returned null");
				}
				
				return new TestResult(getName(), true, "API initialization tests passed");
			} catch (Exception e) {
				return new TestResult(getName(), false, "Exception: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Test configuration validation.
	 */
	private static class ConfigurationValidationTest implements TestCase {
		@Override
		public String getName() {
			return "Configuration Validation";
		}
		
		@Override
		public TestResult execute() {
			try {
				// Test version manager with various config versions
				VersionManager versionManager = new VersionManager("test");
				
				var tooOld = versionManager.validateConfigVersion(0);
				var tooNew = versionManager.validateConfigVersion(999);
				var current = versionManager.validateConfigVersion(3);
				
				if (tooOld == VersionManager.VersionCompatibility.COMPATIBLE) {
					return new TestResult(getName(), false, "Too old version marked as compatible");
				}
				
				if (tooNew == VersionManager.VersionCompatibility.COMPATIBLE) {
					return new TestResult(getName(), false, "Too new version marked as compatible");
				}
				
				if (current != VersionManager.VersionCompatibility.COMPATIBLE) {
					return new TestResult(getName(), false, "Current version not marked as compatible");
				}
				
				return new TestResult(getName(), true, "Configuration validation tests passed");
			} catch (Exception e) {
				return new TestResult(getName(), false, "Exception: " + e.getMessage());
			}
		}
	}
}