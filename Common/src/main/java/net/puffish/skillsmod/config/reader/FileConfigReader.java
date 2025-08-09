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

package net.puffish.skillsmod.config.reader;

import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.util.PathUtils;
import net.puffish.skillsmod.api.util.Result;

import java.nio.file.Files;
import java.nio.file.Path;

public class FileConfigReader extends ConfigReader {
	private final Path modConfigDir;

	public FileConfigReader(Path modConfigDir) {
		this.modConfigDir = modConfigDir;
	}

	public Result<JsonElement, Problem> readFile(Path file) {
		return JsonElement.parseFile(
				file,
				JsonPath.create(modConfigDir.relativize(file).toString())
		);
	}

	@Override
	public Result<JsonElement, Problem> read(Path path) {
		return readFile(modConfigDir.resolve(path));
	}

	@Override
	public boolean exists(Path path) {
		return Files.exists(modConfigDir.resolve(path));
	}
}
