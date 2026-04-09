package net.bluelotuscoding.skillleveling.bridge.forge.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.bluelotuscoding.skillleveling.bridge.forge.client.cnpc.CnpcClientBridge;
import net.bluelotuscoding.skillleveling.bridge.forge.client.cnpc.CnpcDialogView;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public class CnpcDialogueScreen extends Screen {
    private static final int PANEL_WIDTH = 760;
    private static final int PANEL_HEIGHT = 220;
    private static final int PANEL_BOTTOM_MARGIN = 36;
    private static final int PORTRAIT_WIDTH = 132;
    private static final int PORTRAIT_HEIGHT = 156;
    private static final int BUTTON_HEIGHT = 22;
    private static final int TYPEWRITER_CHARS_PER_SECOND = 60;
    private final CnpcDialogView view;
    private final List<ButtonBinding> buttonBindings = new ArrayList<>();
    private boolean submitted;
    private int visibleChars;
    private long lastTypeAt;

    public CnpcDialogueScreen(CnpcDialogView view) {
        super(Text.literal(view.title()));
        this.view = view;
    }

    @Override
    protected void init() {
        clearChildren();
        buttonBindings.clear();
        visibleChars = 0;
        lastTypeAt = Util.getMeasuringTimeMs();

        int panelWidth = Math.min(PANEL_WIDTH, this.width - 48);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height - PANEL_HEIGHT - PANEL_BOTTOM_MARGIN;
        int contentX = panelX + PORTRAIT_WIDTH + 42;
        int contentWidth = panelWidth - PORTRAIT_WIDTH - 72;

        List<DialogButtonSpec> specs = buildButtonSpecs();
        int horizontalGap = 14;
        int verticalGap = 10;
        int horizontalPadding = 36;
        int minButtonWidth = 150;
        int maxButtonWidth = 340;
        int availableWidth = Math.max(0, contentWidth - horizontalPadding);
        boolean canTwoColumn = specs.size() >= 3 && availableWidth >= (minButtonWidth * 2 + horizontalGap);
        int columns = canTwoColumn ? 2 : 1;
        int rawButtonWidth = columns == 2
                ? (availableWidth - horizontalGap) / 2
                : availableWidth;
        int buttonWidth = MathHelper.clamp(rawButtonWidth, minButtonWidth, maxButtonWidth);
        int rows = (int) Math.ceil(specs.size() / (double) columns);
        int gridWidth = columns == 2 ? (buttonWidth * 2) + horizontalGap : buttonWidth;
        int gridX = contentX + Math.max(0, (contentWidth - gridWidth) / 2);
        int gridHeight = rows * BUTTON_HEIGHT + Math.max(0, rows - 1) * verticalGap;
        int gridY = Math.max(panelY + 118, panelY + PANEL_HEIGHT - 28 - gridHeight);

        for (int i = 0; i < specs.size(); i++) {
            DialogButtonSpec spec = specs.get(i);
            int column = columns == 2 ? i % 2 : 0;
            int row = columns == 2 ? i / 2 : i;
            int buttonX = gridX + column * (buttonWidth + horizontalGap);
            int buttonY = gridY + row * (BUTTON_HEIGHT + verticalGap);
            GoldButtonWidget button = new GoldButtonWidget(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT,
                    Text.literal(spec.label()), spec.color(), pressed -> {
                        ButtonBinding binding = new ButtonBinding(spec, pressed);
                        handlePress(binding);
                    });
            this.addDrawableChild(button);
            buttonBindings.add(new ButtonBinding(spec, button));
        }
    }

    @Override
    public void tick() {
        super.tick();
        String body = bodyText();
        if (visibleChars >= body.length()) {
            return;
        }
        long now = Util.getMeasuringTimeMs();
        long elapsed = Math.max(0L, now - lastTypeAt);
        int advance = Math.max(1, (int) (elapsed * TYPEWRITER_CHARS_PER_SECOND / 1000L));
        visibleChars = Math.min(body.length(), visibleChars + advance);
        lastTypeAt = now;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFullyRevealed()) {
            revealAllText();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isFullyRevealed()) {
            revealAllText();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        if (!submitted && !view.hasQuest()) {
            CnpcClientBridge.sendDialogSelected(view.dialogId(), -1);
        }
        super.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int panelWidth = Math.min(PANEL_WIDTH, this.width - 48);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height - PANEL_HEIGHT - PANEL_BOTTOM_MARGIN;
        int contentX = panelX + PORTRAIT_WIDTH + 42;
        int contentWidth = panelWidth - PORTRAIT_WIDTH - 72;

        drawPanel(context, panelX, panelY, panelWidth, PANEL_HEIGHT);
        drawPortrait(context, panelX + 22, panelY + 18);

        int bodyY = panelY + 34;
        context.drawTextWrapped(this.textRenderer, Text.literal(visibleBody()), contentX, bodyY, contentWidth, 0xF4EEE2);

        if (!isFullyRevealed()) {
            int indicatorX = panelX + panelWidth - 32;
            context.drawTextWithShadow(this.textRenderer, Text.literal(">>"), indicatorX, panelY + PANEL_HEIGHT - 28,
                    0xFFE7B865);
        }

        updateButtonState();
        super.render(context, mouseX, mouseY, delta);
        renderNotifications(context);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void handlePress(ButtonBinding binding) {
        if (!isFullyRevealed()) {
            revealAllText();
            return;
        }

        DialogButtonSpec spec = binding.spec();
        submitted = true;
        if (spec.optionSlot() >= 0) {
            CnpcClientBridge.sendDialogSelected(view.dialogId(), spec.optionSlot());
        } else {
            CnpcClientBridge.sendDialogSelected(view.dialogId(), -1);
        }
        close();
    }

    private void updateButtonState() {
        boolean fullyRevealed = isFullyRevealed();
        for (ButtonBinding binding : buttonBindings) {
            binding.button().active = fullyRevealed;
        }
    }

    private List<DialogButtonSpec> buildButtonSpecs() {
        List<DialogButtonSpec> specs = new ArrayList<>();
        List<CnpcDialogView.Option> options = view.options();
        if (options.isEmpty()) {
            specs.add(new DialogButtonSpec("Close", -1, DialogButtonKind.CLOSE, 0xFFFFFFFF));
            return specs;
        }

        for (CnpcDialogView.Option option : options) {
            specs.add(new DialogButtonSpec(sanitizeLabel(option.text(), "Continue"), option.slot(),
                    DialogButtonKind.OPTION, option.color()));
        }
        return specs;
    }

    private void renderNotifications(DrawContext context) {
        CnpcClientBridge.renderAnnouncementOverlay(context, this.textRenderer, this.width, 26);
    }

    private void drawPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x - 3, y - 3, x + width + 3, y + height + 3, 0x40000000);
        context.fill(x, y, x + width, y + height, 0xD30A0A0D);
        context.fill(x, y, x + width, y + 2, 0xFF8F6C33);
        context.fill(x, y + height - 2, x + width, y + height, 0xFF8F6C33);
        context.fill(x, y, x + 2, y + height, 0xFF8F6C33);
        context.fill(x + width - 2, y, x + width, y + height, 0xFF8F6C33);
        context.fill(x + 14, y + height - 52, x + width - 14, y + height - 50, 0x445C4A2F);
    }

    private void drawPortrait(DrawContext context, int x, int y) {
        context.fill(x, y, x + PORTRAIT_WIDTH, y + PORTRAIT_HEIGHT, 0xCC101115);
        context.fill(x, y, x + PORTRAIT_WIDTH, y + 2, 0xFF8F6C33);
        context.fill(x, y + PORTRAIT_HEIGHT - 2, x + PORTRAIT_WIDTH, y + PORTRAIT_HEIGHT, 0xFF8F6C33);
        context.fill(x, y, x + 2, y + PORTRAIT_HEIGHT, 0xFF8F6C33);
        context.fill(x + PORTRAIT_WIDTH - 2, y, x + PORTRAIT_WIDTH, y + PORTRAIT_HEIGHT, 0xFF8F6C33);
        String initials = initials(view.npcName());
        int textWidth = this.textRenderer.getWidth(initials);
        context.drawTextWithShadow(this.textRenderer, Text.literal(initials),
                x + (PORTRAIT_WIDTH - textWidth) / 2,
                y + (PORTRAIT_HEIGHT / 2) - 4,
                0xFFE8D2A3);
        int nameWidth = this.textRenderer.getWidth(view.npcName());
        context.drawTextWithShadow(this.textRenderer, Text.literal(view.npcName()),
                x + (PORTRAIT_WIDTH - nameWidth) / 2,
                y - 12,
                0xFFF1D1);
    }

    private void revealAllText() {
        visibleChars = bodyText().length();
    }

    private boolean isFullyRevealed() {
        return visibleChars >= bodyText().length();
    }

    private String visibleBody() {
        String body = bodyText();
        if (body.isEmpty()) {
            return "";
        }
        return body.substring(0, Math.min(body.length(), visibleChars));
    }

    private String bodyText() {
        return view.body() == null ? "" : view.body();
    }

    private static String sanitizeLabel(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        String normalized = text.trim();
        if (normalized.length() > 26) {
            return normalized.substring(0, 23) + "...";
        }
        return normalized;
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private record DialogButtonSpec(String label, int optionSlot, DialogButtonKind kind, int color) {
    }

    private record ButtonBinding(DialogButtonSpec spec, PressableWidget button) {
    }

    private enum DialogButtonKind {
        OPTION,
        CLOSE
    }

    private final class GoldButtonWidget extends ButtonWidget {
        private final int labelColor;

        private GoldButtonWidget(int x, int y, int width, int height, Text message, int labelColor, PressAction onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.labelColor = labelColor;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            int background = this.active ? (this.isHovered() ? 0xFFE2BE6D : 0xFFD3A14A) : 0xFF8E7A4D;
            int border = this.active ? 0xFF6E5224 : 0xFF5D4B2C;
            int baseTextColor = this.labelColor == 0 ? 0xFFFFFFFF : this.labelColor;
            int textColor = this.active ? baseTextColor : 0xFFCBCBCB;
            context.fill(getX(), getY(), getX() + width, getY() + height, background);
            context.fill(getX(), getY(), getX() + width, getY() + 2, border);
            context.fill(getX(), getY() + height - 2, getX() + width, getY() + height, border);
            context.fill(getX(), getY(), getX() + 2, getY() + height, border);
            context.fill(getX() + width - 2, getY(), getX() + width, getY() + height, border);
            int textWidth = textRenderer.getWidth(getMessage());
            int textX = getX() + (width - textWidth) / 2;
            int textY = getY() + (height - 8) / 2;
            context.drawTextWithShadow(textRenderer, getMessage(), textX, textY, textColor);
        }
    }
}
