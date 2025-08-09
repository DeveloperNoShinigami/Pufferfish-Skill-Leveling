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

package net.puffish.skillsmod.reward;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.reward.RewardFactory;

import java.util.HashMap;
import java.util.Optional;

public class RewardRegistry {
	private static final HashMap<Identifier, RewardFactory> factories = new HashMap<>();

	public static void register(Identifier key, RewardFactory factory) {
		factories.compute(key, (key2, old) -> {
			if (old == null) {
				return factory;
			}
			throw new IllegalStateException("Trying to add duplicate key `" + key + "` to registry");
		});
	}

	public static Optional<RewardFactory> getFactory(Identifier key) {
		return Optional.ofNullable(factories.get(key));
	}
}
