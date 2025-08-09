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

public class SkillUpdateInPacket implements InPacket {
       private final Identifier categoryId;
       private final String skillId;
       private final int level;

       private SkillUpdateInPacket(Identifier categoryId, String skillId, int level) {
               this.categoryId = categoryId;
               this.skillId = skillId;
               this.level = level;
       }

       public static SkillUpdateInPacket read(PacketByteBuf buf) {
               var categoryId = buf.readIdentifier();
               var skillId = buf.readString();
               var level = buf.readInt();
               return new SkillUpdateInPacket(
                               categoryId,
                               skillId,
                               level
               );
       }

	public Identifier getCategoryId() {
		return categoryId;
	}

       public String getSkillId() {
               return skillId;
       }

       public int getLevel() {
               return level;
       }
}
