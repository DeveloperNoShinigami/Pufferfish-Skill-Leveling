package net.puffish.skillsmod.access;

import org.joml.Matrix4f;

import java.util.List;

public interface BuiltBufferAccess {
	void setEmits(List<Matrix4f> emits);

	List<Matrix4f> getEmits();
}
