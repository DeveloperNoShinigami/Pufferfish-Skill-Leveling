package net.puffish.skillsmod.api.integration;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.Events;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.util.PrefixedLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Enhanced integration API that provides additional hooks and events for other mods
 * to integrate with the Pufferfish Skills system.
 */
public class IntegrationAPI {
	private static final PrefixedLogger LOGGER = new PrefixedLogger(SkillsAPI.MOD_ID + ".integration");
	
	private static final Map<String, IntegrationHook> registeredHooks = new ConcurrentHashMap<>();
	private static final Map<String, Consumer<IntegrationEvent>> eventListeners = new ConcurrentHashMap<>();
	
	/**
	 * Registers an integration hook that other mods can use to interact with the skills system.
	 */
	public static void registerIntegrationHook(String hookId, IntegrationHook hook) {
		registeredHooks.put(hookId, hook);
		LOGGER.info("Registered integration hook: {}", hookId);
	}
	
	/**
	 * Gets a registered integration hook by ID.
	 */
	public static IntegrationHook getIntegrationHook(String hookId) {
		return registeredHooks.get(hookId);
	}
	
	/**
	 * Registers a listener for integration events.
	 */
	public static void registerEventListener(String listenerId, Consumer<IntegrationEvent> listener) {
		eventListeners.put(listenerId, listener);
		LOGGER.info("Registered integration event listener: {}", listenerId);
	}
	
	/**
	 * Fires an integration event to all registered listeners.
	 */
	public static void fireEvent(IntegrationEvent event) {
		eventListeners.values().forEach(listener -> {
			try {
				listener.accept(event);
			} catch (Exception e) {
				LOGGER.error("Error in integration event listener: {}", e.getMessage(), e);
			}
		});
	}
	
	/**
	 * Enhanced skill unlock event with more data.
	 */
	public static void fireSkillUnlockEvent(ServerPlayerEntity player, Identifier categoryId, String skillId, Map<String, Object> additionalData) {
		var event = new SkillUnlockEvent(player, categoryId, skillId, additionalData);
		fireEvent(event);
		
		// Also fire the original event for backward compatibility
		SkillsAPI.registerSkillUnlockEvent((catId, skId) -> {
			// This ensures existing listeners still work
		});
	}
	
	/**
	 * Enhanced skill lock event with more data.
	 */
	public static void fireSkillLockEvent(ServerPlayerEntity player, Identifier categoryId, String skillId, Map<String, Object> additionalData) {
		var event = new SkillLockEvent(player, categoryId, skillId, additionalData);
		fireEvent(event);
	}
	
	/**
	 * Fires a custom skill progression event.
	 */
	public static void fireProgressionEvent(ServerPlayerEntity player, Identifier categoryId, String source, int amount, Map<String, Object> additionalData) {
		var event = new SkillProgressionEvent(player, categoryId, source, amount, additionalData);
		fireEvent(event);
	}
	
	/**
	 * Base interface for integration hooks.
	 */
	public interface IntegrationHook {
		String getId();
		boolean isEnabled();
		void onPlayerJoin(ServerPlayerEntity player);
		void onPlayerLeave(ServerPlayerEntity player);
	}
	
	/**
	 * Base class for integration events.
	 */
	public abstract static class IntegrationEvent {
		private final ServerPlayerEntity player;
		private final long timestamp;
		
		protected IntegrationEvent(ServerPlayerEntity player) {
			this.player = player;
			this.timestamp = System.currentTimeMillis();
		}
		
		public ServerPlayerEntity getPlayer() {
			return player;
		}
		
		public long getTimestamp() {
			return timestamp;
		}
		
		public abstract String getEventType();
	}
	
	/**
	 * Enhanced skill unlock event.
	 */
	public static class SkillUnlockEvent extends IntegrationEvent {
		private final Identifier categoryId;
		private final String skillId;
		private final Map<String, Object> additionalData;
		
		public SkillUnlockEvent(ServerPlayerEntity player, Identifier categoryId, String skillId, Map<String, Object> additionalData) {
			super(player);
			this.categoryId = categoryId;
			this.skillId = skillId;
			this.additionalData = additionalData;
		}
		
		@Override
		public String getEventType() {
			return "skill_unlock";
		}
		
		public Identifier getCategoryId() {
			return categoryId;
		}
		
		public String getSkillId() {
			return skillId;
		}
		
		public Map<String, Object> getAdditionalData() {
			return additionalData;
		}
	}
	
	/**
	 * Enhanced skill lock event.
	 */
	public static class SkillLockEvent extends IntegrationEvent {
		private final Identifier categoryId;
		private final String skillId;
		private final Map<String, Object> additionalData;
		
		public SkillLockEvent(ServerPlayerEntity player, Identifier categoryId, String skillId, Map<String, Object> additionalData) {
			super(player);
			this.categoryId = categoryId;
			this.skillId = skillId;
			this.additionalData = additionalData;
		}
		
		@Override
		public String getEventType() {
			return "skill_lock";
		}
		
		public Identifier getCategoryId() {
			return categoryId;
		}
		
		public String getSkillId() {
			return skillId;
		}
		
		public Map<String, Object> getAdditionalData() {
			return additionalData;
		}
	}
	
	/**
	 * Skill progression event for custom triggers.
	 */
	public static class SkillProgressionEvent extends IntegrationEvent {
		private final Identifier categoryId;
		private final String source;
		private final int amount;
		private final Map<String, Object> additionalData;
		
		public SkillProgressionEvent(ServerPlayerEntity player, Identifier categoryId, String source, int amount, Map<String, Object> additionalData) {
			super(player);
			this.categoryId = categoryId;
			this.source = source;
			this.amount = amount;
			this.additionalData = additionalData;
		}
		
		@Override
		public String getEventType() {
			return "skill_progression";
		}
		
		public Identifier getCategoryId() {
			return categoryId;
		}
		
		public String getSource() {
			return source;
		}
		
		public int getAmount() {
			return amount;
		}
		
		public Map<String, Object> getAdditionalData() {
			return additionalData;
		}
	}
}