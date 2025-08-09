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

package net.puffish.skillsmod.impl.json;

import com.google.common.collect.Streams;
import net.puffish.skillsmod.api.json.JsonArray;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.api.util.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public record JsonArrayImpl(
		com.google.gson.JsonArray json,
		JsonPath path
) implements JsonArray {

	@Override
	public Stream<JsonElement> stream() {
		return Streams.mapWithIndex(
				json.asList().stream(),
				(jsonElement, i) -> new JsonElementImpl(jsonElement, path.getArray(i))
		);
	}

	@Override
	public JsonElement getAsElement() {
		return new JsonElementImpl(json, path);
	}

	@Override
	public <S, F> Result<List<S>, List<F>> getAsList(BiFunction<Integer, JsonElement, Result<S, F>> function) {
		var successes = new ArrayList<S>();
		var failures = new ArrayList<F>();

		var tmp = json.asList();
		for (var i = 0; i < tmp.size(); i++) {
			function.apply(i, new JsonElementImpl(tmp.get(i), path.getArray(i)))
					.ifSuccess(successes::add)
					.ifFailure(failures::add);
		}

		if (failures.isEmpty()) {
			return Result.success(successes);
		} else {
			return Result.failure(failures);
		}
	}

	@Override
	public int getSize() {
		return json.size();
	}

	@Override
	public com.google.gson.JsonArray getJson() {
		return json;
	}

	@Override
	public JsonPath getPath() {
		return path;
	}
}
