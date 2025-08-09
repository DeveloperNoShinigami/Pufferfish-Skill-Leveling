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

package net.puffish.skillsmod.server.network.packets.out;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.network.OutPacket;
import net.puffish.skillsmod.network.Packets;

public record SkillUpdateOutPacket(Identifier categoryId, String skillId, int level) implements OutPacket {
       @Override
       public void write(PacketByteBuf buf) {
               buf.writeIdentifier(categoryId);
               buf.writeString(skillId);
               buf.writeInt(level);
       }

       @Override
       public Identifier getId() {
               return Packets.SKILL_UPDATE;
       }
}
