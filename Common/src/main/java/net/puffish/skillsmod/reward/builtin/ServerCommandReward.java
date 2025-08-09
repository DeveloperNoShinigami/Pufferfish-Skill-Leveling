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

package net.puffish.skillsmod.reward.builtin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;

import java.util.Objects;

public class ServerCommandReward implements Reward {
    public static final Identifier ID = SkillsMod.createIdentifier("server_command");

    private final CommandReward delegate;

    private ServerCommandReward(CommandReward delegate) {
        this.delegate = delegate;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, context ->
                CommandReward.parse(context, ServerCommandReward::executeAsServer)
                        .mapSuccess(ServerCommandReward::new)
        );
    }

    private static void executeAsServer(ServerPlayerEntity player, String command) {
        var server = Objects.requireNonNull(player.getServer());
        server.getCommandManager().executeWithPrefix(
                server.getCommandSource()
                        .withSilent()
                        .withLevel(server.getFunctionPermissionLevel()),
                command
        );
    }

    @Override
    public void update(RewardUpdateContext context) {
        delegate.update(context);
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        delegate.dispose(context);
    }
}
