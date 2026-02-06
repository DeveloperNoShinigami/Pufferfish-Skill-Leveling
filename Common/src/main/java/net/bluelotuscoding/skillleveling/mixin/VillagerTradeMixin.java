package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.data.SkillMasterTradeProvider;
import net.bluelotuscoding.skillleveling.registry.ModVillagers;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOfferList;
import net.minecraft.registry.Registries;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public abstract class VillagerTradeMixin {

    @Inject(method = "interactMob", at = @At("HEAD"))
    private void onInteractMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        SkillLevelingMod.getInstance().getLogger()
                .debug("Villager interaction triggered for: " + player.getName().getString());
        if ((Object) this instanceof VillagerEntity villager && !villager.getWorld().isClient) {
            String prof = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession()).toString();
            SkillLevelingMod.getInstance().getLogger().debug("Interaction with villager prof: " + prof);

            if (villager.getVillagerData().getProfession() == ModVillagers.SKILL_MASTER
                    && player instanceof ServerPlayerEntity serverPlayer) {

                int level = villager.getVillagerData().getLevel();

                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                        .debug("Preparing dynamic trades for Skill Master (Level " + level + ") for player "
                                + serverPlayer.getName().getString());

                // Cast to Merchant to safely access getOffers()
                TradeOfferList trades = ((Merchant) this).getOffers();
                trades.clear();

                // Fill with custom + dynamic trades
                SkillMasterTradeProvider.fillTrades(villager, trades, level, serverPlayer);

                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getLogger()
                        .debug("Prepared " + trades.size() + " trades.");
            }
        }
    }
}
