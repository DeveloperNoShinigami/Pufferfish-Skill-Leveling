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

package net.puffish.skillsmod.api.util;

import net.puffish.skillsmod.impl.util.ProblemImpl;

import java.util.Collection;

public interface Problem {
	static Problem message(String message) {
		return new ProblemImpl(message);
	}

	static Problem combine(Collection<Problem> problems) {
		return new ProblemImpl(problems);
	}

	static Problem combine(Problem... problems) {
		return new ProblemImpl(problems);
	}

	@Override
	String toString();
}
