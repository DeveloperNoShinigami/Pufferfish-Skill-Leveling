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

package net.puffish.skillsmod.util;

import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.impl.json.JsonPathImpl;

public class JsonPathFailure {
	public static Problem expectedToExist(JsonPath path) {
		return ((JsonPathImpl) path).expectedToExist();
	}

	public static Problem expectedToExistAndBe(JsonPath path, String str) {
		return ((JsonPathImpl) path).expectedToExistAndBe(str);
	}

	public static Problem expectedToBe(JsonPath path, String str) {
		return ((JsonPathImpl) path).expectedToBe(str);
	}
}
