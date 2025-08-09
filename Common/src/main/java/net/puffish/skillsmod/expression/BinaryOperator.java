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

import java.util.function.BiFunction;

public record BinaryOperator<T>(String token, int precedence, boolean right, BiFunction<Expression<T>, Expression<T>, Expression<T>> function) {

	public static <T> BinaryOperator<T> createLeft(String token, int precedence, BiFunction<Expression<T>, Expression<T>, Expression<T>> function) {
		return new BinaryOperator<>(token, precedence, false, function);
	}

	public static <T> BinaryOperator<T> createRight(String token, int precedence, BiFunction<Expression<T>, Expression<T>, Expression<T>> function) {
		return new BinaryOperator<>(token, precedence, true, function);
	}

}
