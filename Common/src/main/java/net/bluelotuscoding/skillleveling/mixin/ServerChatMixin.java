package net.bluelotuscoding.skillleveling.mixin;

import net.bluelotuscoding.skillleveling.tome.TomePendingActionManager;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts player chat messages to handle Tome input prompts.
 * When a player has a pending Tome action, their chat messages are
 * processed by TomePendingActionManager instead of being broadcast.
 */
@Mixin(value = ServerPlayNetworkHandler.class)
public abstract class ServerChatMixin {

    @Shadow
    public ServerPlayerEntity player;

    /**
     * Intercept chat messages for players with pending tome actions.
     */
    @Inject(method = "handleDecoratedMessage", at = @At("HEAD"), cancellable = true)
    private void onHandleDecoratedMessage(SignedMessage message, CallbackInfo ci) {
        if (player != null && TomePendingActionManager.hasPendingAction(player)) {
            String content = message.getContent().getString();
            boolean consumed = TomePendingActionManager.processPlayerInput(player, content);
            if (consumed) {
                ci.cancel(); // Don't broadcast the message
            }
        }
    }
}
