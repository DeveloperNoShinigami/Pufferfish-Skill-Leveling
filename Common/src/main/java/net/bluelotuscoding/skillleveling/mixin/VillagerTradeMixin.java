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
        if (!((Object) this instanceof VillagerEntity villager) || villager.getWorld().isClient) {
            return;
        }

        if (villager.getVillagerData().getProfession() == ModVillagers.SKILL_MASTER
                && player instanceof ServerPlayerEntity serverPlayer) {

            long currentDay = villager.getWorld().getTime() / 24000L;
            String playerUuid = serverPlayer.getUuidAsString();
            net.minecraft.nbt.NbtCompound tag = villager.writeNbt(new net.minecraft.nbt.NbtCompound());
            
            if (!tag.contains("SkillMasterCache")) {
                tag.put("SkillMasterCache", new net.minecraft.nbt.NbtCompound());
            }
            net.minecraft.nbt.NbtCompound cache = tag.getCompound("SkillMasterCache");
            
            boolean needsRefresh = true;
            if (cache.contains(playerUuid)) {
                net.minecraft.nbt.NbtCompound playerData = cache.getCompound(playerUuid);
                if (playerData.getLong("LastUpdate") == currentDay) {
                    needsRefresh = false;
                }
            }

            TradeOfferList trades = ((Merchant) this).getOffers();
            
            if (needsRefresh) {
                trades.clear();
                int level = villager.getVillagerData().getLevel();
                // Fill with new trades
                SkillMasterTradeProvider.fillTrades(villager, trades, level, serverPlayer);
                
                // Save to cache
                net.minecraft.nbt.NbtCompound playerData = new net.minecraft.nbt.NbtCompound();
                playerData.putLong("LastUpdate", currentDay);
                net.minecraft.nbt.NbtCompound offersNbt = trades.toNbt();
                playerData.put("Offers", offersNbt);
                cache.put(playerUuid, playerData);
                
                // Write back to villager
                tag.put("Offers", offersNbt);
                villager.readNbt(tag);
            } else {
                // Load from cache
                net.minecraft.nbt.NbtCompound playerData = cache.getCompound(playerUuid);
                net.minecraft.nbt.NbtCompound offersNbt = playerData.getCompound("Offers");
                
                // Update the tag and the villager
                tag.put("Offers", offersNbt);
                villager.readNbt(tag);
            }
        }
    }
}
