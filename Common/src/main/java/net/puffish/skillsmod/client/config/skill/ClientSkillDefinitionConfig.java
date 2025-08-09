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

package net.puffish.skillsmod.client.config.skill;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.client.config.ClientFrameConfig;
import net.puffish.skillsmod.client.config.ClientIconConfig;

public record ClientSkillDefinitionConfig(
		String id,
		Identifier type,
		int maxLevels,
		java.util.List<Text> descriptions,
		java.util.List<Text> extraDescriptions,
		Text title,
		ClientIconConfig icon,
                ClientFrameConfig frame,
                float size,
                boolean mergeDescription,
                int cost,
		int requiredSkills,
                int requiredPoints,
                int requiredSpentPoints,
                int requiredExclusions,
                boolean hasLevelRewards
) { }
