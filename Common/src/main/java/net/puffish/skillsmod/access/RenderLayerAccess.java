package net.puffish.skillsmod.access;

import org.joml.Matrix4f;

import java.util.List;

public interface RenderLayerAccess {
	void setEmits(List<Matrix4f> emits);
}
