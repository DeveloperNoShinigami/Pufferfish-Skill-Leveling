package net.bluelotuscoding.skillleveling.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef;
import net.bluelotuscoding.skillleveling.bridge.config.ClassPageDef;
import net.minecraft.network.PacketByteBuf;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Syncs Epic Class bridge metadata (definitions and attribute pages) from
 * server to client.
 */
public class SyncBridgeContentPacket {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final Map<String, EpicClassDef> classDefinitions;
    private final Map<String, List<ClassPageDef>> classAttributePages;

    public SyncBridgeContentPacket(Map<String, EpicClassDef> classDefinitions,
            Map<String, List<ClassPageDef>> classAttributePages) {
        this.classDefinitions = classDefinitions;
        this.classAttributePages = classAttributePages;
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(GSON.toJson(classDefinitions));
        buf.writeString(GSON.toJson(classAttributePages));
    }

    public static SyncBridgeContentPacket read(PacketByteBuf buf) {
        String classesJson = buf.readString(32767 * 8); // Allow for larger content
        String pagesJson = buf.readString(32767 * 8);

        Type classesType = new TypeToken<Map<String, EpicClassDef>>() {
        }.getType();
        Type pagesType = new TypeToken<Map<String, List<ClassPageDef>>>() {
        }.getType();

        Map<String, EpicClassDef> classes = GSON.fromJson(classesJson, classesType);
        Map<String, List<ClassPageDef>> pages = GSON.fromJson(pagesJson, pagesType);

        return new SyncBridgeContentPacket(classes, pages);
    }

    public Map<String, EpicClassDef> getClassDefinitions() {
        return classDefinitions;
    }

    public Map<String, List<ClassPageDef>> getClassAttributePages() {
        return classAttributePages;
    }
}
