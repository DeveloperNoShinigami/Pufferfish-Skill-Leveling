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

package net.puffish.skillsmod.client.data;

import net.puffish.skillsmod.client.config.colors.ClientFillStrokeColorsConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillConfig;

public class ClientSkillConnectionData {
	private final ClientSkillConfig skillA;
	private final ClientSkillConfig skillB;

	private ClientFillStrokeColorsConfig color;

	public ClientSkillConnectionData(
			ClientSkillConfig skillA,
			ClientSkillConfig skillB,
			ClientFillStrokeColorsConfig color
	) {
		this.skillA = skillA;
		this.skillB = skillB;
		this.color = color;
	}

	public ClientSkillConfig getSkillA() {
		return skillA;
	}

	public ClientSkillConfig getSkillB() {
		return skillB;
	}

	public ClientFillStrokeColorsConfig getColor() {
		return color;
	}

	public void setColor(ClientFillStrokeColorsConfig color) {
		this.color = color;
	}
}
