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

public class JobMasterDataLoader extends JsonDataLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Map<String, JobMasterDef> jobMasters = new HashMap<>();

    public JobMasterDataLoader() {
        super(GSON, "epicclassmod/job_masters");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        jobMasters.clear();
        prepared.forEach((id, element) -> {
            try {
                JobMasterDef def = GSON.fromJson(element, JobMasterDef.class);
                if (def != null) {
                    if (def.id == null) {
                        def.id = id.toString();
                    }
                    jobMasters.put(def.id, def);
                }
            } catch (Exception e) {
                AddonLogger.LOGGER.error("Error loading Job Master definition " + id + ": " + e.getMessage());
            }
        });
        AddonLogger.LOGGER.info("Loaded " + jobMasters.size() + " Job Master definitions from datapacks.");

        // Update the manager's registry
        EpicClassConfigManager.setJobMasters(jobMasters);
    }

    public Map<String, JobMasterDef> getJobMasters() {
        return jobMasters;
    }
}
