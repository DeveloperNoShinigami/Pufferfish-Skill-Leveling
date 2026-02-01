package net.bluelotuscoding.skillleveling.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.village.TradeOffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads custom Skill Master trades from datapacks.
 * Path: data/puffish_skill_leveling/skill_master_trades/*.json
 */
public class SkillMasterTradeLoader extends JsonDataLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Map of Tier -> List of possible trade templates
    private final Map<Integer, List<TradeTemplate>> customTrades = new HashMap<>();

    public SkillMasterTradeLoader() {
        super(GSON, "skill_master_trades");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        customTrades.clear();
        prepared.forEach((id, element) -> {
            try {
                JsonObject json = element.getAsJsonObject();
                int tier = json.get("tier").getAsInt();

                TradeTemplate template = parseTemplate(json);
                customTrades.computeIfAbsent(tier, k -> new ArrayList<>()).add(template);
            } catch (Exception e) {
                SkillLevelingMod.getInstance().getLogger()
                        .error("Error loading Skill Master trade " + id + ": " + e.getMessage());
            }
        });
        SkillLevelingMod.getInstance().getLogger().info("Loaded "
                + customTrades.values().stream().mapToLong(List::size).sum() + " custom Skill Master trades.");
    }

    private TradeTemplate parseTemplate(JsonObject json) {
        ItemStack input1 = parseItemStack(json.get("input1").getAsJsonObject());
        ItemStack input2 = json.has("input2") ? parseItemStack(json.get("input2").getAsJsonObject()) : ItemStack.EMPTY;
        ItemStack output = parseItemStack(json.get("output").getAsJsonObject());
        int maxUses = json.has("maxUses") ? json.get("maxUses").getAsInt() : 16;
        int experience = json.has("experience") ? json.get("experience").getAsInt() : 2;
        float multiplier = json.has("multiplier") ? json.get("multiplier").getAsFloat() : 0.05f;

        return new TradeTemplate(input1, input2, output, maxUses, experience, multiplier);
    }

    private ItemStack parseItemStack(JsonObject json) {
        Identifier itemId = new Identifier(json.get("item").getAsString());
        int count = json.has("count") ? Math.max(1, json.get("count").getAsInt()) : 1;
        net.minecraft.item.Item item = net.minecraft.registry.Registries.ITEM.get(itemId);
        return new ItemStack(item, count);
    }

    public List<TradeTemplate> getCustomTradesForTier(int tier) {
        return customTrades.getOrDefault(tier, new ArrayList<>());
    }

    public static class TradeTemplate {
        public final ItemStack input1;
        public final ItemStack input2;
        public final ItemStack output;
        public final int maxUses;
        public final int experience;
        public final float multiplier;

        public TradeTemplate(ItemStack input1, ItemStack input2, ItemStack output, int maxUses, int experience,
                float multiplier) {
            this.input1 = input1;
            this.input2 = input2;
            this.output = output;
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = multiplier;
        }

        public TradeOffer createOffer() {
            return new TradeOffer(input1.copy(), input2.copy(), output.copy(), maxUses, experience, multiplier);
        }
    }
}
