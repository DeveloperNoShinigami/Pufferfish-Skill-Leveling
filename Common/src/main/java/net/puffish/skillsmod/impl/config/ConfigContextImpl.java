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

package net.puffish.skillsmod.impl.config;

import net.minecraft.server.MinecraftServer;
import net.puffish.skillsmod.api.config.ConfigContext;

import java.util.ArrayList;
import java.util.List;

public record ConfigContextImpl(MinecraftServer server, List<String> warnings) implements ConfigContext {
	public ConfigContextImpl(MinecraftServer server) {
		this(server, new ArrayList<>());
	}

	@Override
	public MinecraftServer getServer() {
		return server;
	}

	@Override
	public void emitWarning(String message) {
		warnings.add(message);
	}
}
