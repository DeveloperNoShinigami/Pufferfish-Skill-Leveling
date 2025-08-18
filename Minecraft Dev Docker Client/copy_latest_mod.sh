#!/bin/bash

echo "Copying latest mod builds to test servers..."

# Copy Fabric build
if [ -f "/workspace/Fabric/build/libs/puffish_skill_leveling-*-fabric.jar" ]; then
    cp /workspace/Fabric/build/libs/puffish_skill_leveling-*-fabric.jar /home/minecraft/fabric-server/mods/
    echo "✅ Copied Fabric addon to server"
else
    echo "❌ Fabric jar not found - run ./gradlew :Fabric:build first"
fi

# Copy Forge build  
if [ -f "/workspace/Forge/build/libs/puffish_skill_leveling-*-forge.jar" ]; then
    cp /workspace/Forge/build/libs/puffish_skill_leveling-*-forge.jar /home/minecraft/forge-server/mods/
    echo "✅ Copied Forge addon to server"
else
    echo "❌ Forge jar not found - run ./gradlew :Forge:build first"
fi

echo "Mod copying complete!"
