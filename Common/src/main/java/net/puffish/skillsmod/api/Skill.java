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

package net.puffish.skillsmod.api;

import net.minecraft.server.network.ServerPlayerEntity;

public interface Skill {
	Category getCategory();

	String getId();

	State getState(ServerPlayerEntity player);

	void unlock(ServerPlayerEntity player);

	void lock(ServerPlayerEntity player);

	enum State {
		LOCKED,
		AVAILABLE,
		AFFORDABLE,
		UNLOCKED,
		EXCLUDED
	}
}
