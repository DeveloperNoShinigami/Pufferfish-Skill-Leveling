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

package net.puffish.skillsmod.server.network.packets.in;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.network.InPacket;

public class SkillClickInPacket implements InPacket {
	private final Identifier categoryId;
	private final String skillId;

	private SkillClickInPacket(Identifier categoryId, String skillId) {
		this.categoryId = categoryId;
		this.skillId = skillId;
	}

	public static SkillClickInPacket read(PacketByteBuf buf) {
		return new SkillClickInPacket(
				buf.readIdentifier(),
				buf.readString()
		);
	}

	public Identifier getCategoryId() {
		return categoryId;
	}

	public String getSkillId() {
		return skillId;
	}
}
