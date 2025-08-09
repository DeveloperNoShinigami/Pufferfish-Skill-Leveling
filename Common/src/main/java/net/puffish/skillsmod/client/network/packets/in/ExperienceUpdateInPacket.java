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

package net.puffish.skillsmod.client.network.packets.in;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.network.InPacket;

public class ExperienceUpdateInPacket implements InPacket {
	private final Identifier categoryId;
	private final int currentLevel;
	private final int currentExperience;
	private final int requiredExperience;

	private ExperienceUpdateInPacket(Identifier categoryId, int currentLevel, int currentExperience, int requiredExperience) {
		this.categoryId = categoryId;
		this.currentLevel = currentLevel;
		this.currentExperience = currentExperience;
		this.requiredExperience = requiredExperience;
	}

	public static ExperienceUpdateInPacket read(PacketByteBuf buf) {
		var categoryId = buf.readIdentifier();
		var currentLevel = buf.readInt();
		var currentExperience = buf.readInt();
		var requiredExperience = buf.readInt();

		return new ExperienceUpdateInPacket(
				categoryId,
				currentLevel,
				currentExperience,
				requiredExperience
		);
	}

	public Identifier getCategoryId() {
		return categoryId;
	}

	public int getCurrentLevel() {
		return currentLevel;
	}

	public int getCurrentExperience() {
		return currentExperience;
	}

	public int getRequiredExperience() {
		return requiredExperience;
	}
}
