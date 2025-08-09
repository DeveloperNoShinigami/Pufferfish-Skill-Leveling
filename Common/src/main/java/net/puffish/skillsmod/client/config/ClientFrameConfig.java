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

package net.puffish.skillsmod.client.config;

import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.util.Identifier;

import java.util.Optional;

public sealed interface ClientFrameConfig permits ClientFrameConfig.AdvancementFrameConfig, ClientFrameConfig.TextureFrameConfig {

	record AdvancementFrameConfig(AdvancementFrame frame) implements ClientFrameConfig { }

	record TextureFrameConfig(
			Optional<Identifier> lockedTexture,
			Identifier availableTexture,
			Optional<Identifier> affordableTexture,
			Identifier unlockedTexture,
			Optional<Identifier> excludedTexture
	) implements ClientFrameConfig { }

}
