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
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.puffish.skillsmod.client.SkillsClientMod;
import net.puffish.skillsmod.client.network.ClientPacketHandler;
import net.puffish.skillsmod.client.network.ClientPacketSender;
import net.puffish.skillsmod.client.setup.ClientRegistrar;
import net.puffish.skillsmod.network.InPacket;
import net.puffish.skillsmod.network.OutPacket;

import java.util.Objects;
import java.util.function.Function;

public class ForgeClientMain {

        public ForgeClientMain() {
                SkillsClientMod.setup(
                                new ClientRegistrarImpl(),
                                new ClientPacketSenderImpl()
                );
        }

        private static class ClientRegistrarImpl implements ClientRegistrar {
                @Override
                public <T extends InPacket> void registerInPacket(Identifier identifier, Function<PacketByteBuf, T> reader, ClientPacketHandler<T> handler) {
                        var channel = NetworkRegistry.newEventChannel(
                                        identifier,
                                        () -> "1",
                                        version -> true,
                                        version -> true
                        );
                        channel.addListener(networkEvent -> {
                                var context = networkEvent.getSource().get();
                                if (context.getPacketHandled()) {
                                        return;
                                }
                                if (networkEvent instanceof NetworkEvent.ServerCustomPayloadEvent serverNetworkEvent) {
                                        var packet = reader.apply(serverNetworkEvent.getPayload());
                                        context.enqueueWork(() -> handler.handle(packet));
                                        context.setPacketHandled(true);
                                }
                        });
                }

                @Override
                public void registerOutPacket(Identifier id) { }
        }

        private static class ClientPacketSenderImpl implements ClientPacketSender {
                @Override
                public void send(OutPacket packet) {
                        var buf = new PacketByteBuf(Unpooled.buffer());
                        packet.write(buf);
                        Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler())
                                        .sendPacket(new CustomPayloadC2SPacket(packet.getId(), buf));
                }
        }
}
