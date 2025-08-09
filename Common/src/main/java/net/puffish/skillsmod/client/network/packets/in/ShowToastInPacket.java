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
import net.puffish.skillsmod.network.InPacket;
import net.puffish.skillsmod.util.ToastType;

public class ShowToastInPacket implements InPacket {

	private final ToastType type;

	private ShowToastInPacket(ToastType type) {
		this.type = type;
	}

	public static ShowToastInPacket read(PacketByteBuf buf) {
		return new ShowToastInPacket(buf.readEnumConstant(ToastType.class));
	}

	public ToastType getToastType() {
		return type;
	}
}
