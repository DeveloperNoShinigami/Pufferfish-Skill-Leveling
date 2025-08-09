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

import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.impl.calculation.CalculationImpl;

public interface Calculation<T> {

	static <T> Result<Calculation<T>, Problem> parse(
			JsonElement rootElement,
			Variables<T, Double> variables,
			ConfigContext context
	) {
		return CalculationImpl.create(rootElement, variables, context)
				.mapSuccess(c -> c);
	}

	double evaluate(T t);
}
