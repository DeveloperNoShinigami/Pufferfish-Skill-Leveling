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

package net.puffish.skillsmod.network;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;

public class Packets {
	public static final Identifier SHOW_CATEGORY = SkillsMod.createIdentifier("show_category");
	public static final Identifier HIDE_CATEGORY = SkillsMod.createIdentifier("hide_category");
	public static final Identifier NEW_POINT = SkillsMod.createIdentifier("new_point");
	public static final Identifier SKILL_UPDATE = SkillsMod.createIdentifier("skill_update");
	public static final Identifier POINTS_UPDATE = SkillsMod.createIdentifier("points_update");
	public static final Identifier EXPERIENCE_UPDATE = SkillsMod.createIdentifier("experience_update");
	public static final Identifier SKILL_CLICK = SkillsMod.createIdentifier("skill_click");
	public static final Identifier SHOW_TOAST = SkillsMod.createIdentifier("show_toast");
	public static final Identifier OPEN_SCREEN = SkillsMod.createIdentifier("open_screen");
}
