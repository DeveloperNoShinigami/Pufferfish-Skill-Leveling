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

package net.puffish.skillsmod.calculation.operation.builtin;

import net.minecraft.stat.Stat;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.calculation.operation.Operation;
import net.puffish.skillsmod.api.calculation.operation.OperationConfigContext;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.json.BuiltinJson;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.util.LegacyUtils;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class StatCondition implements Operation<Stat<?>, Boolean> {

	private final Stat<?> stat;

	private StatCondition(Stat<?> stat) {
		this.stat = stat;
	}

	public static void register() {
		BuiltinPrototypes.STAT.registerOperation(
				SkillsMod.createIdentifier("test"),
				BuiltinPrototypes.BOOLEAN,
				StatCondition::parse
		);
	}

	public static Result<StatCondition, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(LegacyUtils.wrapNoUnused(StatCondition::parse, context));
	}

	public static Result<StatCondition, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		var optStat = rootObject.get("stat")
				.andThen(BuiltinJson::parseStat)
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new StatCondition(
					optStat.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<Boolean> apply(Stat<?> stat) {
		return Optional.of(Objects.equals(this.stat, stat));
	}
}
