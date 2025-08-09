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

package net.puffish.skillsmod.api.calculation.operation;

import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import java.util.Optional;
import java.util.function.Function;

public interface OperationFactory<T, R> {
	default <V> OperationFactory<T, V> andThen(Function<? super R, ? extends V> after) {
		return context -> this.apply(context).mapSuccess(o -> v -> o.apply(v).map(after));
	}

	default <V> OperationFactory<V, R> compose(Function<? super V, ? extends T> before) {
		return context -> this.apply(context).mapSuccess(o -> v -> o.apply(before.apply(v)));
	}

	default OperationFactory<Optional<T>, R> optional() {
		return context -> this.apply(context).mapSuccess(o -> t -> t.flatMap(o));
	}

	static <T, R> OperationFactory<T, R> create(Function<T, R> function) {
		return context -> Result.success(t -> Optional.of(function.apply(t)));
	}

	static <T, R> OperationFactory<T, R> createOptional(Operation<T, R> operation) {
		return context -> Result.success(operation);
	}

	Result<? extends Operation<T, R>, Problem> apply(OperationConfigContext context);
}
