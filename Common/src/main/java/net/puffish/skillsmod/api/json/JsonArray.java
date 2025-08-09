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

import net.puffish.skillsmod.api.util.Result;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public interface JsonArray {

	Stream<JsonElement> stream();

	JsonElement getAsElement();

	<S, F> Result<List<S>, List<F>> getAsList(BiFunction<Integer, JsonElement, Result<S, F>> function);

	int getSize();

	JsonPath getPath();

	com.google.gson.JsonArray getJson();
}
