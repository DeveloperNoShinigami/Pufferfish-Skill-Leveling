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

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public sealed interface ClientIconConfig permits ClientIconConfig.EffectIconConfig, ClientIconConfig.ItemIconConfig, ClientIconConfig.TextureIconConfig {

	record ItemIconConfig(ItemStack item) implements ClientIconConfig { }

	record EffectIconConfig(StatusEffect effect) implements ClientIconConfig { }

	record TextureIconConfig(Identifier texture) implements ClientIconConfig { }

}
