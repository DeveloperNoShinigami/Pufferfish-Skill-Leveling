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

package net.puffish.skillsmod.reward.builtin;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;

public class DummyReward implements Reward {
	public static final Identifier ID = SkillsMod.createIdentifier("dummy");

	@Override
	public void update(RewardUpdateContext context) { }

	@Override
	public void dispose(RewardDisposeContext context) { }
}
