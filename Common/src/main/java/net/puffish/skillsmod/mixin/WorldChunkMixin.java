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

import net.minecraft.world.chunk.WorldChunk;
import net.puffish.skillsmod.access.WorldChunkAccess;
import net.puffish.skillsmod.experience.source.builtin.util.AntiFarmingPerChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin implements WorldChunkAccess {
	@Unique
	private final AntiFarmingPerChunk.Data antiFarmingData = new AntiFarmingPerChunk.Data();

	@Override
	public AntiFarmingPerChunk.Data getAntiFarmingData() {
		return antiFarmingData;
	}
}
