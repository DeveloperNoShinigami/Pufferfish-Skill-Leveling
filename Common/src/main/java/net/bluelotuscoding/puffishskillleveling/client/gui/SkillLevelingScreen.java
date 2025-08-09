package net.bluelotuscoding.puffishskillleveling.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.bluelotuscoding.puffishskillleveling.SkillsMod;
import net.bluelotuscoding.puffishskillleveling.client.data.ClientCategoryData;
import net.bluelotuscoding.puffishskillleveling.client.data.ClientSkillScreenData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Extends the base skills screen with an experience bar for the
 * skill leveling addon. All layout and rendering are delegated to the
 * parent screen to avoid duplicated logic.
 */
public class SkillLevelingScreen extends SkillsScreen {
    private static final Identifier ICONS_TEXTURE = new Identifier("textures/gui/icons.png");
    private static final int FRAME_PADDING = 8;
    private static final int TABS_HEIGHT = 28;

    public SkillLevelingScreen(ClientSkillScreenData data, Optional<Identifier> optCategoryId) {
        super(data, optCategoryId);
    }

    @Override
    protected void drawWindowWithCategory(DrawContext context, double mouseX, double mouseY, ClientCategoryData activeCategoryData) {
        super.drawWindowWithCategory(context, mouseX, mouseY, activeCategoryData);

        if (activeCategoryData.hasExperience()) {
            int x;
            int y;
            if (small) {
                x = this.width - FRAME_PADDING - 8 - 182;
                y = TABS_HEIGHT + 25;
            } else {
                x = (this.width - 182) / 2;
                y = TABS_HEIGHT + 15;
            }

            context.drawTexture(ICONS_TEXTURE, x, y, 0, 64, 182, 5);
            int width = Math.min(182, (int) (activeCategoryData.getExperienceProgress() * 183f));
            if (width > 0) {
                context.drawTexture(ICONS_TEXTURE, x, y, 0, 69, width, 5);
            }

            if (isInsideExperience(mouseX, mouseY, x, y)) {
                List<OrderedText> lines = new ArrayList<>();
                var activeCategory = activeCategoryData.getConfig();
                lines.add(SkillsMod.createTranslatable(
                        "tooltip",
                        "current_level",
                        activeCategoryData.getCurrentLevel()
                                + (activeCategory.levelLimit() == Integer.MAX_VALUE ? "" : "/" + activeCategory.levelLimit())
                ).asOrderedText());
                lines.add(SkillsMod.createTranslatable(
                        "tooltip",
                        "experience_progress",
                        activeCategoryData.getCurrentExperience(),
                        activeCategoryData.getRequiredExperience(),
                        MathHelper.floor(activeCategoryData.getExperienceProgress() * 100f)
                ).asOrderedText());
                lines.add(SkillsMod.createTranslatable(
                        "tooltip",
                        "to_next_level",
                        activeCategoryData.getExperienceToNextLevel()
                ).asOrderedText());
                setTooltip(lines);
            }
        }
    }

    private boolean isInsideExperience(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseY >= y && mouseX < x + 182 && mouseY < y + 5;
    }
}

