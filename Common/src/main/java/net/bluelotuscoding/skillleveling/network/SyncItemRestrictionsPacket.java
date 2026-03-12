package net.bluelotuscoding.skillleveling.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.bluelotuscoding.skillleveling.bridge.config.ItemRequirementDef;
import net.bluelotuscoding.skillleveling.bridge.config.ItemRequirementsManager;
import net.minecraft.network.PacketByteBuf;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Syncs item restriction definitions from server to client.
 * Serializes the entire requirements map as a JSON string.
 */
public class SyncItemRestrictionsPacket {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, ItemRequirementDef>>() {
    }.getType();

    private final String itemsJson;
    private final String blocksJson;
    private final String entitiesJson;
    private final String dimensionsJson;
    private final String structuresJson;

    public SyncItemRestrictionsPacket(
            Map<String, ItemRequirementDef> items,
            Map<String, ItemRequirementDef> blocks,
            Map<String, ItemRequirementDef> entities,
            Map<String, ItemRequirementDef> dimensions,
            Map<String, ItemRequirementDef> structures) {
        this.itemsJson = GSON.toJson(items);
        this.blocksJson = GSON.toJson(blocks);
        this.entitiesJson = GSON.toJson(entities);
        this.dimensionsJson = GSON.toJson(dimensions);
        this.structuresJson = GSON.toJson(structures);
    }

    private SyncItemRestrictionsPacket(String i, String b, String e, String d, String s) {
        this.itemsJson = i;
        this.blocksJson = b;
        this.entitiesJson = e;
        this.dimensionsJson = d;
        this.structuresJson = s;
    }

    public static void encode(SyncItemRestrictionsPacket packet, PacketByteBuf buf) {
        buf.writeString(packet.itemsJson);
        buf.writeString(packet.blocksJson);
        buf.writeString(packet.entitiesJson);
        buf.writeString(packet.dimensionsJson);
        buf.writeString(packet.structuresJson);
    }

    public static SyncItemRestrictionsPacket decode(PacketByteBuf buf) {
        return new SyncItemRestrictionsPacket(
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString());
    }

    public void handleClient() {
        Map<String, ItemRequirementDef> items = GSON.fromJson(itemsJson, MAP_TYPE);
        Map<String, ItemRequirementDef> blocks = GSON.fromJson(blocksJson, MAP_TYPE);
        Map<String, ItemRequirementDef> entities = GSON.fromJson(entitiesJson, MAP_TYPE);
        Map<String, ItemRequirementDef> dimensions = GSON.fromJson(dimensionsJson, MAP_TYPE);
        Map<String, ItemRequirementDef> structures = GSON.fromJson(structuresJson, MAP_TYPE);

        if (items != null && blocks != null && entities != null && dimensions != null && structures != null) {
            ItemRequirementsManager.setClientRequirements(items, blocks, entities, dimensions, structures);
        }
    }
}
