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

package net.puffish.skillsmod.experience.source;

import net.puffish.skillsmod.experience.source.builtin.BreakBlockExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.CraftItemExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.DealDamageExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.EatFoodExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.EnchantItemExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.FishItemExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.HealExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.IncreaseStatExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.KillEntityExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.MineBlockExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.SharedKillEntityExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.TakeDamageExperienceSource;

public class BuiltinExperienceSources {
	public static void register() {
		BreakBlockExperienceSource.register();
		CraftItemExperienceSource.register();
		DealDamageExperienceSource.register();
		EatFoodExperienceSource.register();
		EnchantItemExperienceSource.register();
		FishItemExperienceSource.register();
		HealExperienceSource.register();
		IncreaseStatExperienceSource.register();
		KillEntityExperienceSource.register();
		MineBlockExperienceSource.register();
		SharedKillEntityExperienceSource.register();
		TakeDamageExperienceSource.register();
	}
}
