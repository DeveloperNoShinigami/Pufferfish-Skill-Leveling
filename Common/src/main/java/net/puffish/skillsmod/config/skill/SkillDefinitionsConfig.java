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
import net.puffish.skillsmod.util.DisposeContext;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class SkillDefinitionsConfig {
	private final Map<String, SkillDefinitionConfig> definitions;

	private SkillDefinitionsConfig(Map<String, SkillDefinitionConfig> definitions) {
		this.definitions = definitions;
	}

	public static Result<SkillDefinitionsConfig, Problem> parse(JsonElement rootElement, ConfigContext context) {
		return rootElement.getAsObject().andThen(rootObject -> parse(rootObject, context));
	}

       public static Result<SkillDefinitionsConfig, Problem> parse(JsonObject rootObject, ConfigContext context) {
               return rootObject.getAsMap((id, element) -> SkillDefinitionConfig.parse(id, element, context))
                               .mapFailure(problems -> Problem.combine(problems.values()))
                               .mapSuccess(SkillDefinitionsConfig::new);
       }

	public Optional<SkillDefinitionConfig> getById(String id) {
		return Optional.ofNullable(definitions.get(id));
	}

	public Collection<SkillDefinitionConfig> getAll() {
		return definitions.values();
	}

	public void dispose(DisposeContext context) {
		for (var definition : definitions.values()) {
			definition.dispose(context);
		}
	}
}
