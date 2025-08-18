# Quick Mod Testing Setup

## Manual Download Required

Since CurseForge blocks automated downloads, you'll need to manually download the dependencies:

### Required Downloads:

1. **Pufferfish Skills (Fabric)**: 
   - Go to: https://www.curseforge.com/minecraft/mc-mods/puffish-skills
   - Download: Version 0.16.3+1.20 for Fabric
   - Save as: `mods/puffish-skills-fabric.jar`

2. **Pufferfish Skills (Forge)**:
   - Same page, but download the Forge version
   - Save as: `mods/puffish-skills-forge.jar`

3. **Fabric API** (for Fabric testing):
   - Go to: https://www.curseforge.com/minecraft/mc-mods/fabric-api
   - Download: Version 0.95.4+1.20.1
   - Save as: `mods/fabric-api.jar`

## Quick Test Setup

```bash
# 1. Create test directory
mkdir -p test-mods

# 2. Copy your built addon
cp ../Fabric/build/libs/puffish_skill_leveling-0.16.3-1.20-fabric.jar test-mods/

# 3. Download dependencies manually to test-mods/
# (Use the links above)

# 4. Test with any Fabric client/server
# Just put all jars in the mods folder and run
```

## Why Your Mod Wasn't Loading

Your mod requires:
1. **Pufferfish Skills mod** (the dependency)
2. **Fabric API** (for Fabric platform)
3. **Proper mod loading environment**

The issue was that these dependencies were missing from your development environment!

## Quick Verification

To verify your mod is valid, check the jar contents:
```bash
unzip -l ../Fabric/build/libs/puffish_skill_leveling-0.16.3-1.20-fabric.jar | grep -E "(fabric.mod.json|FabricMain)"
```

If you see your main classes and fabric.mod.json, the mod is properly built.
