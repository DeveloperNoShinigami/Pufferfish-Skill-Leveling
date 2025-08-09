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
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.util.LegacyUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CommandReward implements Reward {
        public static final Identifier ID = SkillsMod.createIdentifier("command");

        @FunctionalInterface
        public interface Executor {
                void execute(ServerPlayerEntity player, String command);
        }

        private final Map<UUID, Integer> counts = new HashMap<>();

        private final String command;
        private final String unlockCommand;
        private final String lockCommand;
        private final Executor executor;

        private CommandReward(String command, String unlockCommand, String lockCommand, Executor executor) {
                this.command = command;
                this.unlockCommand = unlockCommand;
                this.lockCommand = lockCommand;
                this.executor = executor;
        }

        public static void register() {
                SkillsAPI.registerReward(
                                ID,
                                context -> parse(context, CommandReward::executeAsPlayer)
                );
        }

        public static Result<CommandReward, Problem> parse(RewardConfigContext context, Executor executor) {
                return context.getData()
                                .andThen(JsonElement::getAsObject)
                                .andThen(LegacyUtils.wrapNoUnused(obj -> parse(obj, executor), context));
        }

        private static Result<CommandReward, Problem> parse(JsonObject rootObject, Executor executor) {
                var problems = new ArrayList<Problem>();

                var command = rootObject.get("command")
                                .getSuccess() // ignore failure because this property is optional
                                .flatMap(jsonElement -> jsonElement.getAsString()
                                                .ifFailure(problems::add)
                                                .getSuccess()
                                )
                                .orElse("");

                var unlockCommand = rootObject.get("unlock_command")
                                .getSuccess() // ignore failure because this property is optional
                                .flatMap(jsonElement -> jsonElement.getAsString()
                                                .ifFailure(problems::add)
                                                .getSuccess()
                                )
                                .orElse("");

                var lockCommand = rootObject.get("lock_command")
                                .getSuccess() // ignore failure because this property is optional
                                .flatMap(jsonElement -> jsonElement.getAsString()
                                                .ifFailure(problems::add)
                                                .getSuccess()
                                )
                                .orElse("");

                if (problems.isEmpty()) {
                        return Result.success(new CommandReward(
                                        command,
                                        unlockCommand,
                                        lockCommand,
                                        executor
                        ));
                } else {
                        return Result.failure(Problem.combine(problems));
                }
        }

        private static void executeAsPlayer(ServerPlayerEntity player, String command) {
                var server = Objects.requireNonNull(player.getServer());

                server.getCommandManager().executeWithPrefix(
                                player.getCommandSource()
                                                .withSilent()
                                                .withLevel(server.getFunctionPermissionLevel()),
                                command
                );
        }

        private void executeCommand(ServerPlayerEntity player, String command) {
                if (command.isBlank()) {
                        return;
                }
                executor.execute(player, command);
        }

	@Override
	public void update(RewardUpdateContext context) {
		var player = context.getPlayer();

		if (context.isAction()) {
			executeCommand(player, command);
		}

                counts.compute(player.getUuid(), (uuid, count) -> {
                        if (count == null) {
                                // initialize without executing commands to avoid
                                // running unlock actions again when the player
                                // rejoins the world
                                return context.getCount();
                        }

                        while (context.getCount() > count) {
                                executeCommand(player, unlockCommand);
                                count++;
                        }
                        while (context.getCount() < count) {
                                executeCommand(player, lockCommand);
                                count--;
                        }

                        return count;
                });
	}

	@Override
	public void dispose(RewardDisposeContext context) {
		for (var entry : counts.entrySet()) {
			var player = context.getServer().getPlayerManager().getPlayer(entry.getKey());
			if (player == null) {
				continue;
			}
			for (var i = 0; i < entry.getValue(); i++) {
				executeCommand(player, lockCommand);
			}
		}
		counts.clear();
	}
}
