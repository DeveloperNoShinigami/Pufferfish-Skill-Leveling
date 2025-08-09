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

package net.puffish.skillsmod.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;

public class SimpleToast implements Toast {
	private final SystemToast toast;

	private SimpleToast(SystemToast toast) {
		this.toast = toast;
	}

	public static SimpleToast create(MinecraftClient client, Text title, Text description) {
		return new SimpleToast(SystemToast.create(client, SystemToast.Type.PACK_LOAD_FAILURE, title, description));
	}

	@Override
	public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
		return toast.draw(context, manager, startTime);
	}

	@Override
	public int getWidth() {
		return toast.getWidth();
	}

	@Override
	public int getHeight() {
		return toast.getHeight();
	}

	@Override
	public int getRequiredSpaceCount() {
		return toast.getRequiredSpaceCount();
	}
}
