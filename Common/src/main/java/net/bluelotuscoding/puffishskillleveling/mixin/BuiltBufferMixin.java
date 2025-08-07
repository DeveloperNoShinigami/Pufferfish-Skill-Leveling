package net.bluelotuscoding.puffishskillleveling.mixin;

import net.minecraft.client.render.BufferBuilder;
import net.bluelotuscoding.puffishskillleveling.access.BuiltBufferAccess;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(BufferBuilder.BuiltBuffer.class)
public class BuiltBufferMixin implements BuiltBufferAccess {
	@Unique
	private List<Matrix4f> emits;

	@Override
	@Unique
	public void setEmits(List<Matrix4f> emits) {
		this.emits = emits;
	}

	@Override
	@Unique
	public List<Matrix4f> getEmits() {
		return emits;
	}
}
