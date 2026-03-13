package net.bluelotuscoding.skillleveling.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
import net.bluelotuscoding.skillleveling.data.ExpTomeConfigLoader;
import net.minecraft.network.PacketByteBuf;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Syncs all addon configurations (leveled skills, exp tomes) from server to client.
 */
public class SyncAllConfigsPacket {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final Map<String, LeveledConfigStorage.LeveledConfig> leveledConfigs;
    private final Map<String, ExpTomeConfigLoader.ExpTomeDefinition> expTomeDefinitions;

    public SyncAllConfigsPacket(Map<String, LeveledConfigStorage.LeveledConfig> leveledConfigs,
            Map<String, ExpTomeConfigLoader.ExpTomeDefinition> expTomeDefinitions) {
        this.leveledConfigs = leveledConfigs;
        this.expTomeDefinitions = expTomeDefinitions;
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(GSON.toJson(leveledConfigs));
        buf.writeString(GSON.toJson(expTomeDefinitions));
    }

    public static SyncAllConfigsPacket read(PacketByteBuf buf) {
        String leveledJson = buf.readString(32767 * 16); // Allow for even larger content
        String expJson = buf.readString(32767 * 16);

        Type leveledType = new TypeToken<Map<String, LeveledConfigStorage.LeveledConfig>>() {}.getType();
        Type expType = new TypeToken<Map<String, ExpTomeConfigLoader.ExpTomeDefinition>>() {}.getType();

        Map<String, LeveledConfigStorage.LeveledConfig> leveled = GSON.fromJson(leveledJson, leveledType);
        Map<String, ExpTomeConfigLoader.ExpTomeDefinition> exp = GSON.fromJson(expJson, expType);

        return new SyncAllConfigsPacket(leveled, exp);
    }

    public Map<String, LeveledConfigStorage.LeveledConfig> getLeveledConfigs() {
        return leveledConfigs;
    }

    public Map<String, ExpTomeConfigLoader.ExpTomeDefinition> getExpTomeDefinitions() {
        return expTomeDefinitions;
    }
}
