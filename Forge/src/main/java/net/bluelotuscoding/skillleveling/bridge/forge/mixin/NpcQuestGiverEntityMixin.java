package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import com.example.epicclassmod.world.entity.NpcQuestGiverEntity;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.bridge.config.JobMasterDef;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NpcQuestGiverEntity.class, remap = false)
public abstract class NpcQuestGiverEntityMixin extends MerchantEntity {

    public NpcQuestGiverEntityMixin(EntityType<? extends MerchantEntity> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract String getNpcId();

    @Inject(method = "applyEquipment", at = @At("HEAD"), cancellable = true)
    private void onApplyEquipment(CallbackInfo ci) {
        String id = getNpcId();
        if (id == null) {
            return;
        }

        JobMasterDef def = EpicClassConfigManager.getJobMaster(id);
        if (def != null && def.equipment != null) {
            ci.cancel();
            // Clear existing
            this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            this.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                    this.equipStack(slot, ItemStack.EMPTY);
                }
            }

            JobMasterDef.Gear gear = def.equipment;
            if (gear.helmet != null) {
                setItemFromId(EquipmentSlot.HEAD, gear.helmet);
            }
            if (gear.chestplate != null) {
                setItemFromId(EquipmentSlot.CHEST, gear.chestplate);
            }
            if (gear.leggings != null) {
                setItemFromId(EquipmentSlot.LEGS, gear.leggings);
            }
            if (gear.boots != null) {
                setItemFromId(EquipmentSlot.FEET, gear.boots);
            }
            if (gear.mainhand != null) {
                setStackInHand(Hand.MAIN_HAND, getItemStackFromId(gear.mainhand));
            }
            if (gear.offhand != null) {
                setStackInHand(Hand.OFF_HAND, getItemStackFromId(gear.offhand));
            }
        }
    }

    @Unique
    private void setItemFromId(EquipmentSlot slot, String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new Identifier(id));
        if (item != null) {
            this.equipStack(slot, new ItemStack(item));
        }
    }

    @Unique
    private ItemStack getItemStackFromId(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new Identifier(id));
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }

    @Inject(method = "openDialogue", at = @At("HEAD"), cancellable = true)
    private void onOpenDialogue(ServerPlayerEntity player, CallbackInfo ci) {
        String id = getNpcId();
        if (id == null) {
            return;
        }

        JobMasterDef def = EpicClassConfigManager.getJobMaster(id);
        if (def != null && def.dialogue_key != null) {
            ci.cancel();
            try {
                Class<?> modNetworkClass = Class.forName("com.example.epicclassmod.network.ModNetwork");
                // Wait, ModNetwork.sendOpenDialogue signature in Yarn mappings?
                // The underlying compiled mod uses whatever Architectury remapped it to,
                // probably still ServerPlayerEntity.
                java.lang.reflect.Method sendOpenDialogue = modNetworkClass.getDeclaredMethod("sendOpenDialogue",
                        ServerPlayerEntity.class, int.class, String.class, boolean.class);
                sendOpenDialogue.setAccessible(true);
                sendOpenDialogue.invoke(null, player, this.getId(), id, false);
            } catch (Exception e) {
                // Log error
            }
        }
    }
}
