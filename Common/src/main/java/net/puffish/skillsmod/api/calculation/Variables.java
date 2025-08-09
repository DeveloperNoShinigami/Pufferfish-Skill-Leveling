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

package net.puffish.skillsmod.api.calculation;

import net.puffish.skillsmod.api.calculation.prototype.Prototype;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.impl.calculation.VariablesImpl;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public interface Variables<T, R> {

	static <T> Result<Variables<T, Double>, Problem> parse(
			JsonElement rootElement,
			Prototype<T> prototype,
			ConfigContext context
	) {
		return VariablesImpl.parse(rootElement, prototype, context);
	}

	static <T, R> Variables<T, R> create(
			Map<String, Function<T, R>> operations
	) {
		return VariablesImpl.create(operations);
	}

	static <T, R> Variables<T, R> combine(
			Collection<Variables<T, R>> variables
	) {
		return VariablesImpl.combine(variables);
	}

	@SafeVarargs
	static <T, R> Variables<T, R> combine(
			Variables<T, R>... variables
	) {
		return VariablesImpl.combine(variables);
	}

	Stream<String> streamNames();

	Map<String, R> evaluate(T t);
}
