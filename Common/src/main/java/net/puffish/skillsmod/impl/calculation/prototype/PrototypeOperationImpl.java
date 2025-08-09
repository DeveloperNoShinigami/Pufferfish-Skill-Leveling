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

package net.puffish.skillsmod.impl.calculation.prototype;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.calculation.operation.Operation;
import net.puffish.skillsmod.api.calculation.operation.OperationConfigContext;
import net.puffish.skillsmod.api.calculation.prototype.PrototypeOperation;
import net.puffish.skillsmod.api.calculation.prototype.Prototype;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import java.util.Optional;

public class PrototypeOperationImpl<T, R> implements PrototypeOperation<T, R> {
	private final Prototype<R> prototype;
	private final Operation<T, R> operation;

	public PrototypeOperationImpl(Prototype<R> prototype, Operation<T, R> operation) {
		this.prototype = prototype;
		this.operation = operation;
	}

	@Override
	public Optional<R> apply(T t) {
		return operation.apply(t);
	}

	@Override
	public Prototype<R> getReturnPrototype() {
		return prototype;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <U> Optional<PrototypeOperation<T, U>> recoverReturnType(Prototype<U> prototype) {
		if (this.prototype == prototype) {
			return Optional.of((PrototypeOperation<T, U>) this);
		}
		return Optional.empty();
	}

	@Override
	public Optional<Result<PrototypeOperation<T, ?>, Problem>> andThen(Identifier id, OperationConfigContext context) {
		return prototype.getOperation(id, context).map(r -> r.mapSuccess(this::andThen));
	}

	private <U> PrototypeOperation<T, ?> andThen(PrototypeOperation<R, U> c) {
		return new PrototypeOperationImpl<>(c.getReturnPrototype(), t -> operation.apply(t).flatMap(c));
	}
}
