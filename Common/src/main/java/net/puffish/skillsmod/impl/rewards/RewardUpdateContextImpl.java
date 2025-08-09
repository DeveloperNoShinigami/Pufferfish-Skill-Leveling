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

package net.puffish.skillsmod.impl.rewards;

import net.minecraft.server.network.ServerPlayerEntity;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;

public record RewardUpdateContextImpl(ServerPlayerEntity player, int count, boolean isAction) implements RewardUpdateContext {

	@Override
	public ServerPlayerEntity getPlayer() {
		return player;
	}

	@Override
	public int getCount() {
		return count;
	}

	@Override
	public boolean isAction() {
		return isAction;
	}

}
