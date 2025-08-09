/*
 * All Rights Reserved
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.puffish.skillsmod.mixin;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.puffish.skillsmod.access.BuiltBufferAccess;
import net.puffish.skillsmod.access.RenderLayerAccess;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

@Mixin(RenderLayer.class)
public final class RenderLayerMixin implements RenderLayerAccess {
	@Unique
	private List<Matrix4f> emits;

	@Override
	@Unique
	public void setEmits(List<Matrix4f> emits) {
		this.emits = emits;
	}

	@ModifyArg(
			method = "draw",
			index = 0,
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/render/BufferRenderer;drawWithGlobalProgram(Lnet/minecraft/client/render/BufferBuilder$BuiltBuffer;)V"
			)
	)
	private BufferBuilder.BuiltBuffer modifyArgAtDrawWithGlobalProgram(BufferBuilder.BuiltBuffer builtBuffer) {
		((BuiltBufferAccess) builtBuffer).setEmits(emits);
		return builtBuffer;
	}
}
