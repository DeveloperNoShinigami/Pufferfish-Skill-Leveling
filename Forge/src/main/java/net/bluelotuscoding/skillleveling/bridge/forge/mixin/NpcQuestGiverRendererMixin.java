package net.bluelotuscoding.skillleveling.bridge.forge.mixin;

import com.example.epicclassmod.client.render.NpcQuestGiverRenderer;
import com.example.epicclassmod.world.entity.NpcQuestGiverEntity;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.bridge.config.JobMasterDef;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NpcQuestGiverRenderer.class, remap = false)
public abstract class NpcQuestGiverRendererMixin
        extends BipedEntityRenderer<NpcQuestGiverEntity, PlayerEntityModel<NpcQuestGiverEntity>> {

    public NpcQuestGiverRendererMixin(EntityRendererFactory.Context context,
            PlayerEntityModel<NpcQuestGiverEntity> entityModel, float f) {
        super(context, entityModel, f);
    }

    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void onGetTextureLocation(NpcQuestGiverEntity entity, CallbackInfoReturnable<Identifier> cir) {
        String id = entity.getNpcId();
        if (id != null) {
            JobMasterDef def = EpicClassConfigManager.getJobMaster(id);
            if (def != null && def.texture != null) {
                cir.setReturnValue(new Identifier(def.texture));
            }
        }
    }
}
