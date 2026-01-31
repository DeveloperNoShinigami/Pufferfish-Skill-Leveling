package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.mixin_interface.SkillLevelHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements SkillLevelHolder {

    @Unique
    private NbtCompound addon$skillLevelingData = new NbtCompound();

    @Override
    public NbtCompound addon$getSkillLevelingData() {
        return addon$skillLevelingData;
    }

    @Override
    public void addon$setSkillLevelingData(NbtCompound nbt) {
        this.addon$skillLevelingData = nbt != null ? nbt : new NbtCompound();
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void onWriteNbt(NbtCompound nbt, CallbackInfo ci) {
        if (!addon$skillLevelingData.isEmpty()) {
            nbt.put("SkillLevelingData", addon$skillLevelingData);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("SkillLevelingData")) {
            addon$skillLevelingData = nbt.getCompound("SkillLevelingData");
        }
    }
}
