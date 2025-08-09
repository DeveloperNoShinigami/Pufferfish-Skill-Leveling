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

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.puffish.skillsmod.access.DamageSourceAccess;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.experience.source.builtin.TakeDamageExperienceSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

	@Inject(
			method = "applyDamage",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/entity/player/PlayerEntity;setHealth(F)V"
			)
	)
	private void injectAtSetHealth(DamageSource source, float damage, CallbackInfo ci) {
		if (((PlayerEntity) (Object) this) instanceof ServerPlayerEntity player) {
			var weapon = ((DamageSourceAccess) source).getWeapon().orElse(ItemStack.EMPTY);
			var takenDamage = Math.min(damage, player.getHealth());

			SkillsAPI.updateExperienceSources(
					player,
					TakeDamageExperienceSource.class,
					experienceSource -> experienceSource.getValue(player, weapon, takenDamage, source)
			);
		}
	}

}
