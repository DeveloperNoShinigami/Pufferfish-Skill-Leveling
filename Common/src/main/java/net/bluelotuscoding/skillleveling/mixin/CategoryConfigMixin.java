package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.config.CategoryConfig;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mixin into CategoryConfig to parse and strip our custom "prerequisite_skills"
 * field from category.json before Pufferfish processes it.
 *
 * Pufferfish wraps category JSON in a JsonObjectTrackingImpl internally,
 * which reports any fields it doesn't consume as "unused". Since categoryJson
 * arrives as a JsonElement (not JsonObject), we cannot access the tracking
 * wrapper directly. Instead, we strip the field from the raw GSON object
 * at HEAD (before Pufferfish sees it) and parse it ourselves.
 */
@Mixin(value = CategoryConfig.class, remap = false)
public abstract class CategoryConfigMixin {

    @Inject(method = "parse", at = @At("HEAD"))
    private static void onParseHead(Identifier id,
            net.puffish.skillsmod.api.json.JsonElement categoryJson,
            net.puffish.skillsmod.api.json.JsonElement definitionsJson,
            net.puffish.skillsmod.api.json.JsonElement skillsJson,
            net.puffish.skillsmod.api.json.JsonElement connectionsJson,
            Optional<net.puffish.skillsmod.api.json.JsonElement> experienceJson,
            ConfigContext context,
            CallbackInfoReturnable<Result<CategoryConfig, Problem>> cir) {

        // Access the raw GSON JsonElement backing the Pufferfish wrapper
        var rawJson = categoryJson.getJson();
        if (!rawJson.isJsonObject()) {
            return;
        }

        var rawObj = rawJson.getAsJsonObject();

        // Parse and remove keep_unlocked field (default: false)
        boolean keepUnlocked = false;
        if (rawObj.has("keep_unlocked")) {
            keepUnlocked = rawObj.remove("keep_unlocked").getAsBoolean();
        }

        if (!rawObj.has("prerequisite_skills")) {
            return;
        }

        // Remove from raw JSON so Pufferfish's tracker never sees it
        var prereqArray = rawObj.remove("prerequisite_skills");

        // Parse our custom field using raw GSON API
        List<LeveledConfigStorage.RequiredSkillEntry> requiredSkills = new ArrayList<>();
        if (prereqArray.isJsonArray()) {
            for (var element : prereqArray.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    var prereqObj = element.getAsJsonObject();

                    String skillId = prereqObj.has("skill")
                            ? prereqObj.get("skill").getAsString()
                            : null;
                    int level = prereqObj.has("level")
                            ? prereqObj.get("level").getAsInt()
                            : 1;
                    String category = prereqObj.has("category")
                            ? prereqObj.get("category").getAsString()
                            : null;

                    if (skillId != null) {
                        requiredSkills.add(
                                new LeveledConfigStorage.RequiredSkillEntry(skillId, level, category));
                    }
                }
            }
        }

        if (!requiredSkills.isEmpty()) {
            LeveledConfigStorage.putCategoryPrerequisites(id, requiredSkills, keepUnlocked);
        }
    }
}
