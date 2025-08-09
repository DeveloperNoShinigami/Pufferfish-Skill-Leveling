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

package net.puffish.skillsmod.calculation.operation;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.calculation.operation.OperationFactory;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.calculation.prototype.Prototype;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.impl.calculation.prototype.PrototypeImpl;

import java.util.Optional;
import java.util.function.Function;

public class LegacyOperationRegistry<T> {
	private final PrototypeImpl<T> prototype;

	public LegacyOperationRegistry(Prototype<T> prototype) {
		this.prototype = (PrototypeImpl<T>) prototype;
	}

	public <U> void registerBooleanFunction(
			String name,
			OperationFactory<U, Boolean> factory,
			Function<T, U> function
	) {
		prototype.registerLegacyOperation(
				createId(name),
				BuiltinPrototypes.NUMBER,
				factory.compose(function).andThen(b -> b ? 1.0 : 0.0)
		);
	}

	public <U> void registerOptionalBooleanFunction(
			String name,
			OperationFactory<U, Boolean> factory,
			Function<T, Optional<U>> function
	) {
		prototype.registerLegacyOperation(
				createId(name),
				BuiltinPrototypes.NUMBER,
				factory.optional().compose(function).andThen(b -> b ? 1.0 : 0.0)
		);
	}

	public <U, R> void registerNumberFunction(
			String name,
			Function<R, Double> postFunction,
			OperationFactory<U, R> factory,
			Function<T, U> function
	) {
		prototype.registerLegacyOperation(
				createId(name),
				BuiltinPrototypes.NUMBER,
				factory.compose(function).andThen(postFunction)
		);
	}

	public void registerNumberFunction(
			String name,
			Function<T, Double> function
	) {
		prototype.registerLegacyOperation(
				createId(name),
				BuiltinPrototypes.NUMBER,
				context -> Result.success(t -> Optional.of(function.apply(t)))
		);
	}

	private Identifier createId(String name) {
		return new Identifier(prototype.getId().getNamespace(), "legacy_" + name);
	}
}
