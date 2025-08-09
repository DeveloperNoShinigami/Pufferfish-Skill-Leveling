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

package net.puffish.skillsmod.api.json;

import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.impl.json.JsonPathImpl;

import java.util.List;
import java.util.Optional;

public interface JsonPath {
	static JsonPath create(String name) {
		return new JsonPathImpl(List.of("`" + name + "`"));
	}

	JsonPath getArray(long index);

	JsonPath getObject(String key);

	Optional<JsonPath> getParent();

	Problem createProblem(String message);

	@Override
	String toString();
}
