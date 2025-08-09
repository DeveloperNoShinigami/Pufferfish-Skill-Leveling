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

import net.puffish.skillsmod.SkillsMod;
import org.apache.commons.io.FileUtils;

import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PathUtils {
	public static boolean isDirectoryEmpty(Path path) {
		try {
			return FileUtils.isEmptyDirectory(path.toFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void copyFileFromJar(Path source, Path target) {
		try {
			FileUtils.copyInputStreamToFile(Objects.requireNonNull(
					SkillsMod.getInstance()
							.getClass()
							.getResourceAsStream("/" + pathToString(source))
			), target.toFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String pathToString(Path path) {
		return StreamSupport.stream(path.spliterator(), false)
				.map(Path::toString)
				.collect(Collectors.joining("/"));
	}
}
