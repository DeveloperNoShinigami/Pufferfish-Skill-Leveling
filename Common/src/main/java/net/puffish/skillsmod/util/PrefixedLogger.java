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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrefixedLogger {
	private final Logger logger;
	private final String prefix;

	public PrefixedLogger(String name) {
		this.logger = LoggerFactory.getLogger(name);
		this.prefix = name;
	}

	private String addPrefix(String str) {
		return "[" + prefix + "] " + str;
	}

	public void info(String str) {
		logger.info(addPrefix(str));
	}

	public void warn(String str) {
		logger.warn(addPrefix(str));
	}

	public void error(String str) {
		logger.error(addPrefix(str));
	}
}
