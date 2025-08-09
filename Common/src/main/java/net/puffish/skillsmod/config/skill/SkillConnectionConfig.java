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

import net.puffish.skillsmod.api.json.JsonArray;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.api.util.Problem;

import java.util.ArrayList;

public record SkillConnectionConfig(
		String skillAId,
		String skillBId
) {

	public static Result<SkillConnectionConfig, Problem> parse(JsonElement rootElement, SkillsConfig skills) {
		return rootElement.getAsArray()
				.andThen(rootArray -> SkillConnectionConfig.parse(rootArray, skills));
	}

	private static Result<SkillConnectionConfig, Problem> parse(JsonArray rootArray, SkillsConfig skills) {
		if (rootArray.getSize() != 2) {
			return Result.failure(rootArray.getPath().createProblem("Expected an array of 2 elements"));
		}

		var problems = new ArrayList<Problem>();

		var optIds = rootArray.getAsList((i, element) -> element.getAsString().andThen(id -> {
					if (skills.getById(id).isEmpty()) {
						return Result.failure(
								element.getPath().createProblem("Expected a valid skill")
						);
					} else {
						return Result.success(id);
					}
				}))
				.ifFailure(problems::addAll)
				.getSuccess();

		if (problems.isEmpty()) {
			var ids = optIds.orElseThrow();
			return Result.success(new SkillConnectionConfig(
					ids.get(0),
					ids.get(1)
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

}
