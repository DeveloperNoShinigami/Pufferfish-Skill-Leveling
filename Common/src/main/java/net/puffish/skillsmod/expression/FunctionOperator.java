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

package net.puffish.skillsmod.expression;

import java.util.List;
import java.util.function.Function;

public record FunctionOperator<T>(String name, String openToken, String separatorToken, String closeToken, int args, Function<List<Expression<T>>, Expression<T>> function) {

	public static <T> FunctionOperator<T> create(String name, String openToken, String separatorToken, String closeToken, int args, Function<List<Expression<T>>, Expression<T>> function) {
		return new FunctionOperator<>(name, openToken, separatorToken, closeToken, args, function);
	}

	public static <T> FunctionOperator<T> createVariadic(String name, String openToken, String separatorToken, String closeToken, Function<List<Expression<T>>, Expression<T>> function) {
		return create(name, openToken, separatorToken, closeToken, -1, function);
	}

}
