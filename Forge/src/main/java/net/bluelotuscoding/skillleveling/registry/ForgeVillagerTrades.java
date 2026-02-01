package net.bluelotuscoding.skillleveling.registry;

import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import net.bluelotuscoding.skillleveling.data.SkillMasterTradeProvider;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffers;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Handles trade registration for the Skill Master profession on Forge.
 */
public class ForgeVillagerTrades {

        @SubscribeEvent
        public void onVillagerTrades(VillagerTradesEvent event) {
                Identifier profId = ForgeRegistries.VILLAGER_PROFESSIONS.getKey(event.getType());
                // Use Identifier comparison as it's more robust during registry setup
                if (SkillLevelingMod.createIdentifier("skill_master").equals(profId)) {
                        SkillLevelingMod.getInstance().getLogger()
                                        .info("Registering trades for Skill Master on Forge...");

                        // Level 1: Proxy Trade (Always required for profession stability)
                        List<TradeOffers.Factory> level1 = event.getTrades().computeIfAbsent(1,
                                        k -> new java.util.ArrayList<>());
                        level1.add((entity, random) -> SkillMasterTradeProvider.createLevel1ProxyTrade(null, null));

                        SkillLevelingMod.getInstance().getLogger()
                                        .info("Added proxy trade to level 1 for Skill Master.");
                }
        }
}
