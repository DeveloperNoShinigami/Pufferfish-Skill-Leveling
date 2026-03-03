package net.bluelotuscoding.skillleveling.bridge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EpicAttributeDataLoader extends JsonDataLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Map<String, List<ClassPageDef>> classAttributePages = new HashMap<>();

    public EpicAttributeDataLoader() {
        super(GSON, "rise_of_heros");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        prepared.forEach((id, element) -> {
            if (!id.getPath().equals("epic_class_attributes"))
                return;

            try {
                if (element.isJsonObject()) {
                    var rootObj = element.getAsJsonObject();
                    if (rootObj.has("attributes_by_class")) {
                        var attributesByClass = rootObj.getAsJsonObject("attributes_by_class");
                        classAttributePages.clear();

                        for (Map.Entry<String, JsonElement> entry : attributesByClass.entrySet()) {
                            String classKey = entry.getKey().toLowerCase(Locale.ROOT);
                            JsonElement classElement = entry.getValue();
                            List<ClassPageDef> pages = new ArrayList<>();

                            if (classElement.isJsonArray()) {
                                for (JsonElement item : classElement.getAsJsonArray()) {
                                    ClassPageDef page = GSON.fromJson(item, ClassPageDef.class);
                                    if (page != null) {
                                        processPage(page);
                                        pages.add(page);
                                    }
                                }
                            }
                            classAttributePages.put(classKey, pages);
                        }
                    }
                }
            } catch (Exception e) {
                AddonLogger.LOGGER.error("Error loading Epic Class attributes for " + id + ": " + e.getMessage());
            }
        });

        AddonLogger.LOGGER.info("Loaded attribute pages for " + classAttributePages.size() + " Epic Classes.");
        EpicClassConfigManager.setAttributePages(classAttributePages);
    }

    private void processPage(ClassPageDef page) {
        if (page.slots != null) {
            for (AttributeDef def : page.slots) {
                if (def != null && def.value != null) {
                    var result = net.puffish.skillsmod.expression.DefaultParser.parse(def.value,
                            java.util.Set.of("points"));
                    def.compiledExpression = result.getSuccess().orElse(null);
                }
            }
        }
    }

    public Map<String, List<ClassPageDef>> getClassAttributePages() {
        return classAttributePages;
    }
}
