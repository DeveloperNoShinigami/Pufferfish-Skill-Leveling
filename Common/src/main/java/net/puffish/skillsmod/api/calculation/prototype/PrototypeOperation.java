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

package net.puffish.skillsmod.api.calculation.prototype;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.calculation.operation.Operation;
import net.puffish.skillsmod.api.calculation.operation.OperationConfigContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.impl.calculation.prototype.PrototypeOperationImpl;

import java.util.Optional;

public interface PrototypeOperation<T, R> extends Operation<T, R> {
	static <U> PrototypeOperation<U, U> createIdentity(Prototype<U> prototype) {
		return new PrototypeOperationImpl<>(prototype, Optional::of);
	}

	Prototype<R> getReturnPrototype();

	<U> Optional<PrototypeOperation<T, U>> recoverReturnType(Prototype<U> prototype);

	Optional<Result<PrototypeOperation<T, ?>, Problem>> andThen(Identifier id, OperationConfigContext context);
}
