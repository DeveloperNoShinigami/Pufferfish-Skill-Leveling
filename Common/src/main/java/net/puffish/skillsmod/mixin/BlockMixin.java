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

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.experience.source.builtin.MineBlockExperienceSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockMixin {

	@Inject(method = "afterBreak", at = @At("HEAD"))
	private void injectAtAfterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack, CallbackInfo ci) {
		if (player instanceof ServerPlayerEntity serverPlayer) {
			SkillsAPI.updateExperienceSources(serverPlayer, experienceSource -> {
				if (experienceSource instanceof MineBlockExperienceSource mineBlockExperienceSource) {
					return mineBlockExperienceSource.getValue(serverPlayer, state, stack);
				}
				return 0;
			});
		}
	}
}
