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

import net.minecraft.advancement.criterion.ConsumeItemCriterion;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.experience.source.builtin.EatFoodExperienceSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConsumeItemCriterion.class)
public class ConsumeItemCriterionMixin {

	@Inject(method = "trigger", at = @At("HEAD"))
	private void injectAtTrigger(ServerPlayerEntity serverPlayer, ItemStack stack, CallbackInfo ci) {
		var food = stack.getItem().getFoodComponent();
		if (food != null) {
			SkillsAPI.updateExperienceSources(
					serverPlayer,
					EatFoodExperienceSource.class,
					experienceSource -> experienceSource.getValue(serverPlayer, stack)
			);
		}
	}
}
