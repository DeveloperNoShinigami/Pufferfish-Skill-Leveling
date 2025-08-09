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

package net.puffish.skillsmod.impl.util;

import net.puffish.skillsmod.api.util.Problem;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProblemImpl implements Problem {
	private final List<String> messages;

	public ProblemImpl(String message) {
		this.messages = List.of(message);
	}

	public ProblemImpl(Collection<Problem> problems) {
		this.messages = problems.stream().flatMap(ProblemImpl::streamMessages).toList();
	}

	public ProblemImpl(Problem... problems) {
		this.messages = Arrays.stream(problems).flatMap(ProblemImpl::streamMessages).toList();
	}

	@Override
	public String toString() {
		return messages.stream().collect(Collectors.joining(System.lineSeparator()));
	}

	public static Stream<String> streamMessages(Problem problem) {
		if (problem instanceof ProblemImpl impl) {
			return impl.messages.stream();
		} else {
			return Stream.of(problem.toString());
		}
	}
}
