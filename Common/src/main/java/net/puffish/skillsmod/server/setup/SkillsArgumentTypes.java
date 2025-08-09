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

package net.puffish.skillsmod.server.setup;

import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.commands.arguments.CategoryArgumentType;
import net.puffish.skillsmod.commands.arguments.SkillArgumentType;

public class SkillsArgumentTypes {
	public static void register(ServerRegistrar registrar) {
		registrar.registerArgumentType(
				SkillsMod.createIdentifier("category"),
				CategoryArgumentType.class,
				new CategoryArgumentType.Serializer()
		);
		registrar.registerArgumentType(
				SkillsMod.createIdentifier("skill"),
				SkillArgumentType.class,
				new SkillArgumentType.Serializer()
		);
	}
}
