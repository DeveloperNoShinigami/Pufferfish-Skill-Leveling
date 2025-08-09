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

package net.puffish.skillsmod.client.network.packets.in;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.network.InPacket;

import java.util.Optional;

public class OpenScreenInPacket implements InPacket {
	private final Optional<Identifier> categoryId;

	private OpenScreenInPacket(Optional<Identifier> categoryId) {
		this.categoryId = categoryId;
	}

	public static OpenScreenInPacket read(PacketByteBuf buf) {
		return new OpenScreenInPacket(buf.readOptional(PacketByteBuf::readIdentifier));
	}

	public Optional<Identifier> getCategoryId() {
		return categoryId;
	}
}
