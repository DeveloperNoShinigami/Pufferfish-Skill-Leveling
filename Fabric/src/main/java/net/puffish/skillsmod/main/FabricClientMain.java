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

package net.puffish.skillsmod.main;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.client.SkillsClientMod;
import net.puffish.skillsmod.client.network.ClientPacketHandler;
import net.puffish.skillsmod.client.network.ClientPacketSender;
import net.puffish.skillsmod.client.setup.ClientRegistrar;
import net.puffish.skillsmod.network.InPacket;
import net.puffish.skillsmod.network.OutPacket;

import java.util.function.Function;

public class FabricClientMain implements ClientModInitializer {

        @Override
        public void onInitializeClient() {
                SkillsClientMod.setup(
                                new ClientRegistrarImpl(),
                                new ClientPacketSenderImpl()
                );
        }

	private static class ClientRegistrarImpl implements ClientRegistrar {
		@Override
		public <T extends InPacket> void registerInPacket(Identifier id, Function<PacketByteBuf, T> reader, ClientPacketHandler<T> handler) {
			ClientPlayNetworking.registerGlobalReceiver(
					id,
					(client, handler2, buf, responseSender) -> {
						var packet = reader.apply(buf);
						client.execute(() -> handler.handle(packet));
					}
			);
		}

		@Override
		public void registerOutPacket(Identifier id) { }
	}

        private static class ClientPacketSenderImpl implements ClientPacketSender {
                @Override
                public void send(OutPacket packet) {
                        var buf = new PacketByteBuf(Unpooled.buffer());
                        packet.write(buf);
                        ClientPlayNetworking.send(packet.getId(), buf);
                }
        }
}
