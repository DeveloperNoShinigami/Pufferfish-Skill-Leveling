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

package net.puffish.skillsmod.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class RefundCommandExtension {
        public static void register() {
                SkillsCommand.REFUND_EXTENSION.register(builder -> {
                        builder.then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                                        .executes(ctx -> SkillsCommand.refund(ctx, IntegerArgumentType.getInteger(ctx, "count"))));
                        builder.then(CommandManager.literal("all")
                                        .executes(RefundCommandExtension::refundAll));
                });
        }

        private static int refundAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                return SkillsCommand.refundAll(context);
        }
}
