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

import org.joml.Vector2i;

public record Bounds2i(Vector2i min, Vector2i max) {
	public static Bounds2i zero() {
		return new Bounds2i(new Vector2i(0, 0), new Vector2i(0, 0));
	}

	public void extend(Vector2i p) {
		min.min(p);
		max.max(p);
	}

	public void grow(int d) {
		min.sub(d, d);
		max.add(d, d);
	}

	public int width() {
		return max.x() - min.x();
	}

	public int height() {
		return max.y() - min.y();
	}
}
