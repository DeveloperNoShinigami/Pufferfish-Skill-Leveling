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

package net.puffish.skillsmod.reward.builtin;

import net.minecraft.server.MinecraftServer;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PerLevelRewardsRewardTest {

	private static class DummyContext implements RewardConfigContext {
	    private final JsonElement data;

	    private DummyContext(JsonElement data) {
	        this.data = data;
	    }

	    @Override
	    public MinecraftServer getServer() {
	        return null;
	    }

	    @Override
	    public void emitWarning(String message) {
	        // ignore
	    }

	    @Override
	    public Result<JsonElement, Problem> getData() {
	        return Result.success(data);
	    }
	}

	private static Result<PerLevelRewardsReward, Problem> parse(String json) {
	    var element = JsonElement.parseString(json, JsonPath.create("test"))
	            .getSuccess().orElseThrow();
	    return PerLevelRewardsReward.parse(new DummyContext(element));
	}

        @Test
        public void testValidValues() {
            String json = "{\"max_skill_level\":1,\"points_per_level\":0,\"levels\":{\"1\":[]}}";
            var result = parse(json);
            Assertions.assertEquals(1, result.getSuccess().orElseThrow().getMaxLevel());
        }

        @Test
        public void testLevelsInferMaxLevel() {
            String json = "{\"levels\":{\"1\":[],\"2\":[]}}";
            var result = parse(json);
            var reward = result.getSuccess().orElseThrow();
            Assertions.assertEquals(2, reward.getMaxLevel());
            Assertions.assertEquals(0, reward.getPointsPerLevel());
        }

        @Test
        public void testInvalidMaxLevel() {
            String json = "{\"max_skill_level\":0,\"points_per_level\":0,\"levels\":{\"1\":[]}}";
            var result = parse(json);
            Assertions.assertTrue(result.getFailure().isPresent());
        }

        @Test
        public void testInvalidPointsPerLevel() {
            String json = "{\"max_skill_level\":1,\"points_per_level\":-1,\"levels\":{\"1\":[]}}";
            var result = parse(json);
            Assertions.assertTrue(result.getFailure().isPresent());
        }
}
