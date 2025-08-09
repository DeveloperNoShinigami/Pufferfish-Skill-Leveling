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

import net.puffish.skillsmod.api.json.JsonArray;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public class ForwardingJsonObject implements JsonObject {
    protected final JsonObject parent;

    public ForwardingJsonObject(JsonObject parent) {
        this.parent = parent;
    }

    @Override
    public Result<JsonElement, Problem> get(String key) {
        return parent.get(key);
    }

    @Override
    public Result<JsonObject, Problem> getObject(String key) {
        return parent.getObject(key);
    }

    @Override
    public Result<JsonArray, Problem> getArray(String key) {
        return parent.getArray(key);
    }

    @Override
    public Result<String, Problem> getString(String key) {
        return parent.getString(key);
    }

    @Override
    public Result<Float, Problem> getFloat(String key) {
        return parent.getFloat(key);
    }

    @Override
    public Result<Double, Problem> getDouble(String key) {
        return parent.getDouble(key);
    }

    @Override
    public Result<Integer, Problem> getInt(String key) {
        return parent.getInt(key);
    }

    @Override
    public Result<Boolean, Problem> getBoolean(String key) {
        return parent.getBoolean(key);
    }

    @Override
    public Stream<Map.Entry<String, JsonElement>> stream() {
        return parent.stream();
    }

    @Override
    public JsonElement getAsElement() {
        return parent.getAsElement();
    }

    @Override
    public <S, F> Result<Map<String, S>, Map<String, F>> getAsMap(BiFunction<String, JsonElement, Result<S, F>> function) {
        return parent.getAsMap(function);
    }

    @Override
    public <S> Result<S, Problem> noUnused(Function<JsonObject, Result<S, Problem>> function) {
        return parent.noUnused(function);
    }

    @Override
    public JsonPath getPath() {
        return parent.getPath();
    }

    @Override
    public com.google.gson.JsonObject getJson() {
        return parent.getJson();
    }
}

