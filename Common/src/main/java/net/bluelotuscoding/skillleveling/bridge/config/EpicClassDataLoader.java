package net.bluelotuscoding.skillleveling.bridge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.HashMap;
import java.util.Map;

public class EpicClassDataLoader extends JsonDataLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Map<Identifier, EpicClassDef> classes = new HashMap<>();

    public EpicClassDataLoader() {
        super(GSON, "rise_of_heros/classes");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        classes.clear();
        prepared.forEach((id, element) -> {
            try {
                EpicClassDef def = GSON.fromJson(element, EpicClassDef.class);
                if (def != null) {
                    if (def.class_name == null) {
                        def.class_name = id.toString();
                    }
                    classes.put(id, def);
                }
            } catch (Exception e) {
                AddonLogger.LOGGER.error("Error loading Epic Class definition " + id + ": " + e.getMessage());
            }
        });
        AddonLogger.LOGGER.info("Loaded " + classes.size() + " Epic Class definitions from datapacks.");

        // Update the manager's registry
        EpicClassConfigManager.setClasses(classes);
    }

    public Map<Identifier, EpicClassDef> getClasses() {
        return classes;
    }
}
