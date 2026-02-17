# Tier 1 — Category-to-Class Mapping Bridge

## Overview

Tier 1 establishes a **data-driven link** between Pufferfish Skills categories and Epic Class Mod's class system. When a player selects a class in Epic Class Mod, the corresponding Pufferfish category is automatically activated (gated/ungated). When viewed the other way, a Pufferfish category can declare "I represent Warrior" so the bridge knows the association.

This tier is **non-invasive**: it does not modify any Epic Class Mod code, screens, or enums. It is purely event-driven and config-driven.

---

## Architecture

```
┌─────────────────────┐       ┌──────────────────────┐
│  Epic Class Mod     │       │  Pufferfish Skills    │
│                     │       │  Addon                │
│  Player chooses     │──────▶│  Bridge Listener      │
│  ClassType.WARRIOR  │ event │  maps WARRIOR →       │
│                     │       │  category "warrior"   │
│  PlayerClassData    │       │                       │
│  (NBT storage)      │       │  Activates category,  │
│                     │       │  grants starting      │
└─────────────────────┘       │  skills, etc.         │
                              └──────────────────────┘
```

### Data Flow

1. Player opens `ClassSelectScreen`, picks a class, clicks "Select"
2. `ClassSelectScreen.choose()` sends `ChooseClassPacket` to server via `ModNetwork.CHANNEL`
3. Server-side `ChooseClassPacket` handler sets `PlayerClassData.classType` in player NBT
4. **Our bridge** listens for the class change event (or capability sync) and maps it to a Pufferfish category
5. Category activation triggers any configured starting skills, rewards, etc.

---

## Key Epic Class Mod Internals (Reference)

### ClassType Enum
```java
// com.example.epicclassmod.data.PlayerClassData$ClassType
public enum ClassType {
    NONE,      // ordinal 0 — no class selected
    WARRIOR,   // ordinal 1
    PALADIN,   // ordinal 2
    BERSERKER, // ordinal 3
    REAPER,    // ordinal 4
    SORCERER,  // ordinal 5
    ARCHER     // ordinal 6
}
```

### PlayerClassData (Capability)
- Stored as Forge Capability on the player entity
- NBT key: `"ClassType"` → integer (enum ordinal)
- Provider: `PlayerClassData` implements `ICapabilitySerializable`
- Access: `player.getCapability(ModCapabilities.CLASS_DATA).ifPresent(...)`

### Network Protocol
- `ChooseClassPacket`: Client → Server, sends `ClassType` enum
  - Constructor: `new ChooseClassPacket(ClassType type)`
  - Serialization: writes `type.ordinal()` as int
  - Server handler: sets `PlayerClassData.classType`, syncs back via `SyncClassPacket`
- `SyncClassPacket`: Server → Client, client receives `ClassType` ordinal

### choose() Method (ClassSelectScreen)
```java
private void choose(ClassType type) {
    ClientClassState.selectedType = type;                    // client cache
    ModNetwork.CHANNEL.sendToServer(new ChooseClassPacket(type)); // → server
    Minecraft.getInstance().setScreen(null);                 // close screen
    // ... schedules a main dialogue quest after 2500ms
}
```

---

## Implementation Plan

### 1. Bridge Configuration File

Create a JSON config that maps Epic Class Mod class types to Pufferfish category IDs:

**File: `config/pufferfish_skills_bridge.json`**
```json
{
    "epicClassBridge": {
        "enabled": true,
        "classToCategoryMap": {
            "WARRIOR": "my_server:warrior",
            "PALADIN": "my_server:paladin",
            "BERSERKER": "my_server:berserker",
            "REAPER": "my_server:reaper",
            "SORCERER": "my_server:sorcerer",
            "ARCHER": "my_server:archer"
        },
        "autoActivateCategory": true,
        "lockOtherCategories": true,
        "syncOnLogin": true
    }
}
```

### 2. Bridge Listener (Server-Side)

**File: `Common/src/main/java/net/puffish/skillsmod/bridge/EpicClassBridge.java`**

```java
package net.puffish.skillsmod.bridge;

import net.puffish.skillsmod.SkillsAPI;
import net.puffish.skillsmod.api.Category;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.EnumMap;
import java.util.Optional;

/**
 * Core bridge logic — maps Epic Class Mod ClassType values
 * to Pufferfish Skills category IDs.
 *
 * This class is loader-agnostic. Platform-specific event
 * registration lives in Forge/Fabric subprojects.
 */
public class EpicClassBridge {

    // Populated from config
    private static final Map<String, ResourceLocation> CLASS_TO_CATEGORY = new HashMap<>();
    private static boolean enabled = false;
    private static boolean autoActivate = true;
    private static boolean lockOthers = true;

    /**
     * Called when config loads. Populates the mapping.
     */
    public static void loadConfig(BridgeConfig config) {
        CLASS_TO_CATEGORY.clear();
        enabled = config.enabled;
        autoActivate = config.autoActivateCategory;
        lockOthers = config.lockOtherCategories;

        for (var entry : config.classToCategoryMap.entrySet()) {
            CLASS_TO_CATEGORY.put(
                entry.getKey().toUpperCase(),
                new ResourceLocation(entry.getValue())
            );
        }
    }

    /**
     * Called when a player's Epic Class changes.
     * @param player The server player
     * @param classTypeName The ClassType enum name (e.g. "WARRIOR")
     */
    public static void onClassChanged(ServerPlayer player, String classTypeName) {
        if (!enabled) return;

        ResourceLocation categoryId = CLASS_TO_CATEGORY.get(classTypeName.toUpperCase());
        if (categoryId == null) {
            // No mapping for this class — log and skip
            LOGGER.debug("No Pufferfish category mapped for class: {}", classTypeName);
            return;
        }

        // Activate the mapped category
        if (autoActivate) {
            SkillsAPI.activateCategory(player, categoryId);
        }

        // Optionally lock all other mapped categories
        if (lockOthers) {
            for (var entry : CLASS_TO_CATEGORY.entrySet()) {
                if (!entry.getKey().equals(classTypeName.toUpperCase())) {
                    SkillsAPI.deactivateCategory(player, entry.getValue());
                }
            }
        }
    }

    /**
     * Reverse lookup: given a Pufferfish category ID,
     * return the Epic Class name it maps to (if any).
     */
    public static Optional<String> getClassForCategory(ResourceLocation categoryId) {
        for (var entry : CLASS_TO_CATEGORY.entrySet()) {
            if (entry.getValue().equals(categoryId)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /**
     * Get the category mapped to a class.
     */
    public static Optional<ResourceLocation> getCategoryForClass(String classTypeName) {
        ResourceLocation id = CLASS_TO_CATEGORY.get(classTypeName.toUpperCase());
        return Optional.ofNullable(id);
    }

    public static boolean isEnabled() { return enabled; }
    public static Map<String, ResourceLocation> getMappings() {
        return Collections.unmodifiableMap(CLASS_TO_CATEGORY);
    }
}
```

### 3. Forge Event Hook

**File: `Forge/src/main/java/net/puffish/skillsmod/bridge/ForgeEpicClassEventHandler.java`**

The challenge: Epic Class Mod does NOT fire a custom event when the class changes. The `ChooseClassPacket` handler directly sets the capability.

**Strategy A — Mixin into ChooseClassPacket handler:**

```java
@Mixin(targets = "com.example.epicclassmod.network.ChooseClassPacket")
public class ChooseClassPacketMixin {

    /**
     * Inject at the TAIL of the server handler to detect class choice.
     * The handler method is a lambda/method reference inside ChooseClassPacket.
     * We need to identify the exact method name via bytecode analysis.
     *
     * Alternative: Mixin into PlayerClassData.setClassType() if it exists,
     * or into the deserialization point.
     */
    @Inject(method = "handle", at = @At("TAIL"))
    private void onClassChosen(/* params */, CallbackInfo ci) {
        // Extract the ClassType that was set
        // Notify EpicClassBridge.onClassChanged(player, classTypeName)
    }
}
```

**Strategy B — Polling via PlayerTickEvent (simpler, no Mixin needed):**

```java
@SubscribeEvent
public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    if (!(event.player instanceof ServerPlayer sp)) return;
    if (sp.tickCount % 20 != 0) return; // Check once per second

    // Read current Epic Class from capability
    sp.getCapability(/* ModCapabilities.CLASS_DATA */).ifPresent(data -> {
        String currentClass = data.getClassType().name();
        String cached = cachedClasses.get(sp.getUUID());

        if (!currentClass.equals(cached)) {
            cachedClasses.put(sp.getUUID(), currentClass);
            if (cached != null) { // Skip first tick (login)
                EpicClassBridge.onClassChanged(sp, currentClass);
            }
        }
    });
}
```

**Strategy C — Mixin into PlayerClassData capability (recommended):**

```java
@Mixin(targets = "com.example.epicclassmod.data.PlayerClassData")
public class PlayerClassDataMixin {

    @Inject(
        method = "setClassType", // or the NBT deserialization method
        at = @At("TAIL")
    )
    private void onClassTypeChanged(CallbackInfo ci) {
        // 'this' is PlayerClassData
        // We need to get the player entity from context
        // Then call EpicClassBridge.onClassChanged(...)
    }
}
```

### 4. Login Sync

When a player logs in, read their existing Epic Class from the capability and ensure their Pufferfish category state matches:

```java
@SubscribeEvent
public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer sp)) return;
    if (!EpicClassBridge.isEnabled()) return;

    sp.getCapability(/* ModCapabilities.CLASS_DATA */).ifPresent(data -> {
        String className = data.getClassType().name();
        if (!"NONE".equals(className)) {
            EpicClassBridge.onClassChanged(sp, className);
        }
    });
}
```

### 5. Reverse Sync (Optional)

If a player unlocks a category through Pufferfish's own system, optionally set their Epic Class:

```java
/**
 * When a Pufferfish category is activated, check if it maps to
 * an Epic Class and set it on the player.
 */
public static void onCategoryActivated(ServerPlayer player, ResourceLocation categoryId) {
    if (!enabled) return;

    getClassForCategory(categoryId).ifPresent(className -> {
        player.getCapability(/* CLASS_DATA */).ifPresent(data -> {
            // Use reflection or accessor mixin to call:
            // data.setClassType(ClassType.valueOf(className));
            // Then sync to client via ModNetwork
        });
    });
}
```

---

## Capability Access Pattern

Epic Class Mod registers capabilities via `ClassPersistEvents`:

```java
// Decompiled from ClassPersistEvents.class
@SubscribeEvent
public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
    if (event.getObject() instanceof Player) {
        event.addCapability(
            new ResourceLocation("epicclassmod", "class_data"),
            new PlayerClassData.Provider()
        );
        event.addCapability(
            new ResourceLocation("epicclassmod", "level_data"),
            new PlayerLevelData.Provider()
        );
    }
}
```

To access from outside the mod:
```java
// Get the capability key (via reflection or AT)
Capability<PlayerClassData> CLASS_DATA_CAP = CapabilityManager.get(
    new CapabilityToken<PlayerClassData>(){}
);

// Or use the ResourceLocation-based lookup
player.getCapability(CLASS_DATA_CAP).ifPresent(data -> {
    PlayerClassData.ClassType type = data.getClassType();
    // ...
});
```

Since `PlayerClassData` and `ClassType` are in a different mod, we need to either:
1. **Add epicclassmod as a compile dependency** (soft dep — `compileOnly`)
2. **Use reflection** to access the capability and class type
3. **Use an Access Transformer** to widen visibility

**Recommended:** Add as `compileOnly` dependency and use `@Mod.EventBusSubscriber(modid = "puffish_skills")` with a check for `ModList.get().isLoaded("epicclassmod")`.

---

## Dependency Management

### build.gradle.kts (Forge)
```kotlin
dependencies {
    // Soft dependency — only for compilation, not bundled
    compileOnly(files("libs/epicclassmod-1.7.8.jar"))

    // OR via repository if available
    // compileOnly("com.example:epicclassmod:1.7.8")
}
```

### mods.toml (Forge)
```toml
[[dependencies.puffish_skills]]
    modId = "epicclassmod"
    mandatory = false          # Soft dependency
    versionRange = "[1.7.0,)"
    ordering = "AFTER"
    side = "BOTH"
```

### Conditional Loading
```java
public class EpicClassBridgeLoader {
    public static void init() {
        if (ModList.get().isLoaded("epicclassmod")) {
            EpicClassBridge.loadConfig(readConfig());
            MinecraftForge.EVENT_BUS.register(ForgeEpicClassEventHandler.class);
            LOGGER.info("Epic Class Mod detected — bridge enabled");
        } else {
            LOGGER.info("Epic Class Mod not found — bridge disabled");
        }
    }
}
```

---

## File Summary

| File | Purpose |
|------|---------|
| `Common/.../bridge/EpicClassBridge.java` | Core mapping logic (loader-agnostic) |
| `Common/.../bridge/BridgeConfig.java` | Config POJO for JSON deserialization |
| `Forge/.../bridge/ForgeEpicClassEventHandler.java` | Forge event hooks (tick/login) |
| `Forge/.../bridge/EpicClassBridgeLoader.java` | Conditional mod detection + init |
| `config/pufferfish_skills_bridge.json` | User-editable mapping config |
| *(Optional)* `Forge/.../mixin/ChooseClassPacketMixin.java` | Direct injection for instant detection (vs polling) |

---

## Testing Checklist

- [ ] Player selects Warrior in Epic Class Mod → "warrior" category activates in Pufferfish
- [ ] Player selects a different class → old category deactivates, new one activates
- [ ] Player logs in with existing class → category state is restored
- [ ] Config reload updates mappings without restart
- [ ] Epic Class Mod not installed → bridge silently disabled, no crashes
- [ ] Unmapped class type → logged at debug level, no error
- [ ] Reverse sync (optional): activating Pufferfish category sets Epic Class

---

## Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Epic Class Mod updates ClassType enum | Medium | Use string-based mapping, not ordinals |
| Capability access breaks across versions | Medium | Wrap in try-catch, version check |
| Polling approach has 1-second delay | Low | Acceptable for class selection (one-time) |
| Mixin into ChooseClassPacket is fragile | Medium | Fall back to polling if mixin fails |
| Race condition: Pufferfish not loaded yet | Low | Check SkillsAPI readiness before activation |

---

## Next Steps → Tier 2

Once Tier 1 is stable, Tier 2 adds a **Skill Tree tab** to Epic Class Mod's `ClassBookScreen`, allowing players to view their Pufferfish skill tree directly from the class book interface. See [BRIDGE_TIER2_TAB_INJECTION.md](BRIDGE_TIER2_TAB_INJECTION.md).
