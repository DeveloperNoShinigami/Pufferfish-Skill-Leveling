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

package net.puffish.skillsmod.calculation.operation.builtin.legacy;

import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.entry.RegistryEntryList;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.calculation.operation.Operation;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.calculation.operation.OperationConfigContext;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.BuiltinJson;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import java.util.ArrayList;
import java.util.Optional;

public final class LegacyDamageTypeTagCondition implements Operation<DamageType, Boolean> {
	private final RegistryEntryList<DamageType> entries;

	private LegacyDamageTypeTagCondition(RegistryEntryList<DamageType> entries) {
		this.entries = entries;
	}

	public static void register() {
		BuiltinPrototypes.DAMAGE_TYPE.registerOperation(
				SkillsMod.createIdentifier("legacy_damage_type_tag"),
				BuiltinPrototypes.BOOLEAN,
				LegacyDamageTypeTagCondition::parse
		);
	}

	public static Result<LegacyDamageTypeTagCondition, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(rootObject -> parse(rootObject, context));
	}

	public static Result<LegacyDamageTypeTagCondition, Problem> parse(JsonObject rootObject, ConfigContext context) {
		var problems = new ArrayList<Problem>();

		var optTag = rootObject.get("tag")
				.andThen(element -> BuiltinJson.parseDamageTypeTag(element, context.getServer().getRegistryManager()))
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new LegacyDamageTypeTagCondition(
					optTag.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<Boolean> apply(DamageType damageType) {
		return Optional.of(entries.stream().anyMatch(entry -> entry.value() == damageType));
	}
}
