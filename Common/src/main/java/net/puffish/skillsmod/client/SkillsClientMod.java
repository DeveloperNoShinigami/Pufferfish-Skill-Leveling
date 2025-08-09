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

package net.puffish.skillsmod.client;

import net.puffish.skillsmod.client.network.ClientPacketSender;
import net.puffish.skillsmod.client.setup.ClientRegistrar;

public class SkillsClientMod {
    private static SkillsClientMod instance;

    private final ClientPacketSender packetSender;

    private SkillsClientMod(ClientPacketSender packetSender) {
        this.packetSender = packetSender;
    }

    public static void setup(ClientRegistrar registrar, ClientPacketSender packetSender) {
        instance = new SkillsClientMod(packetSender);
    }

    public static SkillsClientMod getInstance() {
        return instance;
    }

    public ClientPacketSender getPacketSender() {
        return packetSender;
    }
}

