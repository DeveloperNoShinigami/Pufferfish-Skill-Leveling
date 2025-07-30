package net.puffish.skill_leveling.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.puffish.skill_leveling.access.MinecraftClientAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin implements MinecraftClientAccess {
	@Shadow
	@Final
	private BufferBuilderStorage bufferBuilders;

	@Override
	public BufferBuilderStorage getBufferBuilders() {
		return bufferBuilders;
	}
}
