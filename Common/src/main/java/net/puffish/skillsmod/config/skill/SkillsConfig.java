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

package net.puffish.skillsmod.config.skill;

import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class SkillsConfig {
	private final Map<String, SkillConfig> skills;

	public SkillsConfig(Map<String, SkillConfig> skills) {
		this.skills = skills;
	}

	public static Result<SkillsConfig, Problem> parse(JsonElement rootElement, SkillDefinitionsConfig definitions, ConfigContext context) {
		return rootElement.getAsObject().andThen(rootObject -> SkillsConfig.parse(rootObject, definitions, context));
	}

	public static Result<SkillsConfig, Problem> parse(JsonObject rootObject, SkillDefinitionsConfig definitions, ConfigContext context) {
		return rootObject.getAsMap((key, value) -> SkillConfig.parse(key, value, definitions, context))
				.mapFailure(problems -> Problem.combine(problems.values()))
				.mapSuccess(SkillsConfig::new);
	}

	public Optional<SkillConfig> getById(String id) {
		return Optional.ofNullable(skills.get(id));
	}

	public Collection<SkillConfig> getAll() {
		return skills.values();
	}

	public Map<String, SkillConfig> getMap() {
		return skills;
	}
}
