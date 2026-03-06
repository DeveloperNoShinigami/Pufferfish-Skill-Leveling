package net.bluelotuscoding.skillleveling.loot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.config.LeveledConfigStorage;
import net.bluelotuscoding.skillleveling.util.ImbuedSkillHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.math.random.Random;
import java.util.*;

/**
 * Manages the random imbuing of skills onto loot items.
 * Loads configuration from datapacks under 'skill_imbue_loot' directory.
 */
public class LootImbueManager extends JsonDataLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final LootImbueConfig config = new LootImbueConfig();
    private final Random random = Random.create();

    public LootImbueManager() {
        super(GSON, "skill_imbue_loot");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        config.categories.clear();
        config.global.clear();

        SkillLevelingMod.getInstance().getLogger().info("Loading Skill Imbue Loot Configuration...");
        for (var entry : prepared.entrySet()) {
            try {
                JsonElement element = entry.getValue();
                if (element.isJsonObject()) {
                    parseConfig(element.getAsJsonObject());
                }
            } catch (Exception e) {
                SkillLevelingMod.getInstance().getLogger()
                        .error("Error parsing skill imbue loot " + entry.getKey() + ": " + e.getMessage());
            }
        }
        SkillLevelingMod.getInstance().getLogger()
                .info("Skill Imbue Loot Config Loaded: " + config.dimensionOverrides.size() + " dimensions, "
                        + config.categories.size() + " categories, " + config.global.size() + " global skills");

    }

    private void parseConfig(JsonObject json) {
        // Parse root-level settings
        if (json.has("distance_scaling")) {
            JsonObject scaling = json.getAsJsonObject("distance_scaling");
            if (scaling.has("enabled"))
                config.distanceScaling.enabled = scaling.get("enabled").getAsBoolean();
            if (scaling.has("origin")) {
                JsonArray origin = scaling.getAsJsonArray("origin");
                if (origin.size() >= 2) {
                    config.distanceScaling.origin[0] = origin.get(0).getAsInt();
                    config.distanceScaling.origin[1] = origin.get(1).getAsInt();
                }
            }
            if (scaling.has("brackets")) {
                for (JsonElement e : scaling.getAsJsonArray("brackets")) {
                    JsonObject b = e.getAsJsonObject();
                    var bracket = new LootImbueConfig.Bracket();
                    if (b.has("distance"))
                        bracket.distance = b.get("distance").getAsInt();
                    if (b.has("max_level"))
                        bracket.maxLevel = b.get("max_level").getAsInt();
                    if (b.has("chance_mult"))
                        bracket.chanceMult = b.get("chance_mult").getAsDouble();
                    config.distanceScaling.brackets.add(bracket);
                }
                // Sort brackets by distance desc? Or asc? Usually asc to find range.
                config.distanceScaling.brackets.sort(Comparator.comparingInt(b -> b.distance));
            }
        }

        if (json.has("dimension_overrides")) {
            JsonObject overrides = json.getAsJsonObject("dimension_overrides");
            for (String dim : overrides.keySet()) {
                JsonObject o = overrides.getAsJsonObject(dim);
                var override = new LootImbueConfig.DimensionOverride();
                if (o.has("imbue_chance"))
                    override.imbueChance = o.get("imbue_chance").getAsDouble();
                if (o.has("max_skills"))
                    override.maxSkills = o.get("max_skills").getAsInt();
                if (o.has("min_level"))
                    override.minLevel = o.get("min_level").getAsInt();
                if (o.has("max_level"))
                    override.maxLevel = o.get("max_level").getAsInt();
                config.dimensionOverrides.put(dim, override);
            }
        }

        if (json.has("item_blacklist")) {
            for (JsonElement e : json.getAsJsonArray("item_blacklist")) {
                config.itemBlacklist.add(e.getAsString());
            }
        }

        if (json.has("item_whitelist")) {
            for (JsonElement e : json.getAsJsonArray("item_whitelist")) {
                config.itemWhitelist.add(e.getAsString());
            }
        }

        if (json.has("loot_table_whitelist")) {
            for (JsonElement e : json.getAsJsonArray("loot_table_whitelist")) {
                config.lootTableWhitelist.add(e.getAsString());
            }
        }

        if (json.has("category_settings")) {
            JsonObject settings = json.getAsJsonObject("category_settings");
            for (String cat : settings.keySet()) {
                JsonObject s = settings.getAsJsonObject(cat);
                var setting = new LootImbueConfig.CategorySettings();
                if (s.has("imbue_chance"))
                    setting.imbueChance = s.get("imbue_chance").getAsDouble();
                if (s.has("min_level"))
                    setting.minLevel = s.get("min_level").getAsInt();
                if (s.has("max_level"))
                    setting.maxLevel = s.get("max_level").getAsInt();
                if (s.has("max_skills"))
                    setting.maxSkills = s.get("max_skills").getAsInt();
                config.categorySettings.put(cat, setting);
            }
        }

        if (json.has("exclusion_groups")) {
            for (JsonElement e : json.getAsJsonArray("exclusion_groups")) {
                JsonObject g = e.getAsJsonObject();
                if (g.has("types")) {
                    LootImbueConfig.ExclusionGroup group = new LootImbueConfig.ExclusionGroup();
                    for (JsonElement t : g.getAsJsonArray("types")) {
                        group.types.add(t.getAsString());
                    }
                    config.exclusionGroups.add(group);
                }
            }
        }

        if (json.has("global")) {
            parseEntries(json.getAsJsonArray("global"), config.global);
        }

        // Parse category lists (dynamic keys)
        // Parse category lists (dynamic keys)
        for (String key : json.keySet()) {
            if (key.equals("global") || key.equals("distance_scaling") || key.equals("dimension_overrides")
                    || key.equals("item_blacklist") || key.equals("item_whitelist")
                    || key.equals("loot_table_whitelist") || key.equals("category_settings")
                    || key.equals("exclusion_groups"))
                continue;

            JsonElement element = json.get(key);
            if (element.isJsonArray()) {
                List<LootImbueConfig.ImbueEntry> entries = config.categories.computeIfAbsent(key,
                        k -> new ArrayList<>());
                parseEntries(element.getAsJsonArray(), entries);
            }
        }
    }

    private void parseEntries(JsonArray array, List<LootImbueConfig.ImbueEntry> target) {
        for (JsonElement e : array) {
            JsonObject obj = e.getAsJsonObject();
            String skill = obj.get("skill").getAsString();
            int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 10;
            target.add(new LootImbueConfig.ImbueEntry(skill, weight));
        }
    }

    /**
     * Attempts to apply random skill imbuement(s) to the stack based on context.
     * 
     * @param lootTableId The ID of the loot table being queried, or null if
     *                    unknown/not applicable.
     */
    public void applyRandomImbue(ItemStack stack, LootContext context, @Nullable Identifier lootTableId) {
        applyRandomImbue(stack,
                context.getWorld(),
                context.get(LootContextParameters.ORIGIN),
                context.get(LootContextParameters.THIS_ENTITY),
                context.getRandom(),
                lootTableId);
    }

    /**
     * Direct imbuement call for when LootContext is not available (e.g. Forge
     * events).
     */
    public void applyRandomImbue(ItemStack stack, net.minecraft.world.World world, @Nullable Vec3d origin,
            @Nullable net.minecraft.entity.Entity entity, Random random, @Nullable Identifier lootTableId) {
        String itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();

        if (config.itemBlacklist.contains(itemId)) {
            return;
        }

        // Check whitelist if present
        if (!config.itemWhitelist.isEmpty()) {
            boolean whitelisted = false;
            for (String allowed : config.itemWhitelist) {
                if (LootCategory.matches(stack, allowed)) {
                    whitelisted = true;
                    break;
                }
            }
            if (!whitelisted) {
                return;
            }
        } else {
            // Implicit Whitelist: If no explicit whitelist, ONLY allow valid
            // Equipment/Charms.
            LootCategory cat = LootCategory.forItem(stack);
            if (cat == LootCategory.NONE) {
                return;
            }
        }

        if (!config.lootTableWhitelist.isEmpty()) {
            if (lootTableId == null || !config.lootTableWhitelist.contains(lootTableId.toString())) {
                return;
            }
        }

        String dimId = world.getRegistryKey().getValue().toString();
        LootImbueConfig.DimensionOverride override = config.dimensionOverrides.get(dimId);

        // If not configured for this dimension, do nothing
        if (override == null) {
            return;
        }

        // Determine effective chance and limits
        double chance = override.imbueChance;
        int maxSkills = override.maxSkills;

        // Check for category-specific settings
        LootImbueConfig.CategorySettings catSettings = null;
        for (String cat : config.categorySettings.keySet()) {
            if (LootCategory.matches(stack, cat)) {
                catSettings = config.categorySettings.get(cat);
                break;
            }
        }

        if (catSettings != null) {
            if (catSettings.imbueChance >= 0)
                chance = catSettings.imbueChance;
            if (catSettings.maxSkills >= 0)
                maxSkills = catSettings.maxSkills;
        }

        if (random.nextDouble() > chance) {
            return;
        }

        int skillCount = 1;
        if (maxSkills > 1) {
            skillCount = 1 + random.nextInt(maxSkills);
        }


        // Ensure max slot count on item is sufficient
        if (ImbuedSkillHelper.getSlotCount(stack) < skillCount) {
            ImbuedSkillHelper.setSlotCount(stack, Math.max(ImbuedSkillHelper.getSlotCount(stack), skillCount));
        }

        List<String> addedSkills = new ArrayList<>();
        for (int i = 0; i < skillCount; i++) {
            applySingleImbue(stack, world, origin, entity, random, override, catSettings, addedSkills);
        }
    }

    private void applySingleImbue(ItemStack stack, net.minecraft.world.World world, @Nullable Vec3d origin,
            @Nullable net.minecraft.entity.Entity entity, Random random, LootImbueConfig.DimensionOverride override,
            LootImbueConfig.CategorySettings catSettings, List<String> addedSkills) {
        List<LootImbueConfig.ImbueEntry> pool = new ArrayList<>();

        // Add matching category skills
        for (Map.Entry<String, List<LootImbueConfig.ImbueEntry>> entry : config.categories.entrySet()) {
            if (LootCategory.matches(stack, entry.getKey())) {
                pool.addAll(entry.getValue());
            }
        }
        // Add global skills
        pool.addAll(config.global);

        if (pool.isEmpty()) {
            return;
        }

        // Filter pool based on exclusion groups and already added skills
        List<LootImbueConfig.ImbueEntry> validPool = new ArrayList<>();
        for (LootImbueConfig.ImbueEntry entry : pool) {
            if ("any".equals(entry.skill)) {
                // Expand "any" into all registered imbuable skills
                for (java.util.Map.Entry<String, net.bluelotuscoding.skillleveling.config.LeveledConfigStorage.LeveledConfig> cfgEntry : net.bluelotuscoding.skillleveling.config.LeveledConfigStorage
                        .getAllEntries().entrySet()) {
                    var cfg = cfgEntry.getValue();
                    if ("both".equals(cfg.lootMode) || "imbue_only".equals(cfg.lootMode)) {
                        String fullId = cfgEntry.getKey();
                        if (canAddSkill(fullId, addedSkills)) {
                            validPool.add(new LootImbueConfig.ImbueEntry(fullId, entry.weight));
                        }
                    }
                }
            } else if (canAddSkill(entry.skill, addedSkills)) {
                validPool.add(entry);
            }
        }

        if (validPool.isEmpty())
            return;

        // Weighted selection
        int totalWeight = validPool.stream().mapToInt(e -> e.weight).sum();
        if (totalWeight <= 0)
            return;

        int r = random.nextInt(totalWeight);
        int current = 0;
        for (LootImbueConfig.ImbueEntry entry : validPool) {
            current += entry.weight;
            if (r < current) {
                // Determined skill
                var resolved = net.bluelotuscoding.skillleveling.util.SkillResolver.resolve(entry.skill);
                if (resolved.isEmpty())
                    break; // Skip if invalid

                String skillId = resolved.get().fullId();
                String categoryId = resolved.get().categoryId();
                var leveledConfig = resolved.get().config();

                // Determine level
                int minLevel = override.minLevel;
                int maxLevel = override.maxLevel;

                if (catSettings != null) {
                    if (catSettings.minLevel >= 0)
                        minLevel = catSettings.minLevel;
                    if (catSettings.maxLevel >= 0)
                        maxLevel = catSettings.maxLevel;
                }

                int effectiveMax = Math.min(maxLevel, leveledConfig != null ? leveledConfig.maxLevels : maxLevel);
                int effectiveMin = Math.min(minLevel, effectiveMax);

                int level = effectiveMin;
                if (effectiveMax > effectiveMin) {
                    level += random.nextInt(effectiveMax - effectiveMin + 1);
                }
                level = Math.max(1, level);

                // Distance scaling
                if (config.distanceScaling.enabled) {
                    if (origin != null) {
                        double dist = Math.sqrt(Math.pow(origin.x - config.distanceScaling.origin[0], 2)
                                + Math.pow(origin.z - config.distanceScaling.origin[1], 2));
                        // Find matching bracket
                        int maxDistLevel = Integer.MAX_VALUE;
                        boolean found = false;
                        for (LootImbueConfig.Bracket b : config.distanceScaling.brackets) {
                            if (dist < b.distance) {
                                maxDistLevel = b.maxLevel;
                                found = true;
                                break;
                            }
                        }
                        if (!found && !config.distanceScaling.brackets.isEmpty()) {
                            maxDistLevel = config.distanceScaling.brackets
                                    .get(config.distanceScaling.brackets.size() - 1).maxLevel;
                        }

                        if (found || !config.distanceScaling.brackets.isEmpty()) {
                            level = Math.min(level, maxDistLevel);
                        }
                    }
                }

                // Clamp to skill max level
                if (leveledConfig != null) {
                    level = Math.min(level, leveledConfig.maxLevels);
                }
                level = Math.max(1, level);

                ImbuedSkillHelper.addSkill(stack, categoryId, skillId, level);
                addedSkills.add(skillId);
                break;
            }
        }
    }

    private boolean canAddSkill(String newSkill, List<String> currentSkills) {
        if (currentSkills.contains(newSkill))
            return false;

        for (LootImbueConfig.ExclusionGroup group : config.exclusionGroups) {
            if (matchesExclusion(newSkill, group)) {
                for (String existing : currentSkills) {
                    if (matchesExclusion(existing, group))
                        return false;
                }
            }
        }
        return true;
    }

    private boolean matchesExclusion(String skillId, LootImbueConfig.ExclusionGroup group) {
        if (group.types.contains(skillId))
            return true;
        var config = LeveledConfigStorage.get(skillId);
        if (config != null && group.types.contains(config.categoryId))
            return true;
        return false;
    }
}
