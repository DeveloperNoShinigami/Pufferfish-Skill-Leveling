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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public abstract class VillagerTradeMixin {

    @Inject(method = "interactMob", at = @At("HEAD"))
    private void onInteractMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if ((Object) this instanceof VillagerEntity villager && !villager.getWorld().isClient) {
            if (villager.getVillagerData().getProfession() == ModVillagers.SKILL_MASTER
                    && player instanceof ServerPlayerEntity serverPlayer) {

                int level = villager.getVillagerData().getLevel();

                // Cast to Merchant to safely access getOffers()
                TradeOfferList trades = ((Merchant) this).getOffers();
                trades.clear();

                // Fill with custom + dynamic trades
                SkillMasterTradeProvider.fillTrades(villager, trades, level, serverPlayer);
            }
        }
    }
}
