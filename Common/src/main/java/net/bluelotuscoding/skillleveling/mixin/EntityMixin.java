package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.bridge.config.ItemRequirementDef;
import net.bluelotuscoding.skillleveling.bridge.config.ItemRequirementsManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "moveToWorld", at = @At("HEAD"), cancellable = true)
    private void onMoveToWorld(ServerWorld destination, CallbackInfoReturnable<Entity> cir) {
        Entity entity = (Entity) (Object) this;

        if (entity instanceof ServerPlayerEntity player) {
            String dimId = destination.getRegistryKey().getValue().toString();

            ItemRequirementDef def = ItemRequirementsManager.getRequirements(dimId,
                    ItemRequirementsManager.TargetType.DIMENSION);
            if (def != null) {
                List<String> failures = ItemRequirementsManager.checkRequirements(player, dimId,
                        ItemRequirementsManager.TargetType.DIMENSION);

                if (!failures.isEmpty()) {
                    // Block teleportation
                    player.sendMessage(Text.literal("\u00A7c\u00A7l\u2716 Dimension Restricted:").formatted(), true);
                    for (String failure : failures) {
                        player.sendMessage(Text.literal("\u00A7c  \u2022 " + failure), false);
                    }

                    // If it's a portal, we might need to push the player back a bit to prevent
                    // recursive triggers
                    player.getServer().execute(() -> {
                        player.refreshPositionAfterTeleport(player.getX(), player.getY(), player.getZ());
                        // Optional: push back slightly?
                    });

                    cir.setReturnValue(null);
                }
            }
        }
    }
}
