package net.bluelotuscoding.skillleveling.bridge.forge.client.cnpc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.bluelotuscoding.skillleveling.bridge.network.CnpcQuestAbandonPacket;
import net.bluelotuscoding.skillleveling.bridge.network.CnpcQuestTurnInPacket;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcNpcRoleInfo;
import net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcNpcRoleResolver;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestIntegrationResolver;
import net.bluelotuscoding.skillleveling.bridge.forge.client.screen.CnpcDialogueScreen;
import net.bluelotuscoding.skillleveling.client.CnpcClientQuestState;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public final class CnpcClientBridge {
    private static final String CNPC_PACKETS_CLASS = "noppes.npcs.packets.Packets";
    private static final String DIALOG_SELECTED_PACKET_CLASS = "noppes.npcs.packets.server.SPacketDialogSelected";
    private static final long ANNOUNCEMENT_DURATION_MS = 3600L;
    private static final int ANNOUNCEMENT_CARD_WIDTH = 300;
    private static final int ANNOUNCEMENT_CARD_HEIGHT = 72;

    private static Method sendServerMethod;
    private static final Deque<OverlayAnnouncement> ACTIVE_ANNOUNCEMENTS = new ArrayDeque<>();
    private static final Set<Long> RENDER_LOGGED = new HashSet<>();

    private CnpcClientBridge() {
    }

    public static boolean isMirroredQuest(String questId) {
        return CnpcQuestIntegrationResolver.isCnpcQuestModeEnabled()
                && questId != null
                && (CnpcQuestIntegrationResolver.getQuestMapping(questId) != null
                        || CnpcClientQuestState.getQuest(questId) != null);
    }

    public static boolean isMirroredNpc(Object npc) {
        if (!CnpcQuestIntegrationResolver.isCnpcQuestModeEnabled() || !(npc instanceof Entity entity)) {
            return false;
        }
        CnpcNpcRoleInfo cached = CnpcClientNpcState.get(entity.getId());
        if (cached != null) {
            return cached.hasAnyRole();
        }
        return CnpcNpcRoleResolver.resolve(entity).hasAnyRole();
    }

    public static void enqueueQuestAnnouncement(long sequence, String questTitle, boolean completed) {
        enqueueOverlayAnnouncement(new OverlayAnnouncement(
                sequence,
                completed ? "Quest Complete" : "Quest Accepted",
                normalizeTitle(questTitle),
                Util.getMeasuringTimeMs() + ANNOUNCEMENT_DURATION_MS));
    }

    public static void showQuestNotReady(String questTitle) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("Quest is not ready to turn in: " + normalizeTitle(questTitle)),
                    true);
        }
    }

    public static boolean sendQuestCompletionCheck(String questId) {
        if (questId == null || questId.isBlank()) {
            return false;
        }
        try {
            int parsedQuestId = Integer.parseInt(questId.trim());
            ForgeNetworkHandler.CHANNEL.sendToServer(new CnpcQuestTurnInPacket(parsedQuestId));
            return true;
        } catch (Exception e) {
            AddonLogger.LOGGER
                    .warn("Failed to send CNPC quest completion check for " + questId + ": " + e);
            return false;
        }
    }

    public static boolean sendQuestAbandon(String questId) {
        if (questId == null || questId.isBlank()) {
            return false;
        }
        try {
            int parsedQuestId = Integer.parseInt(questId.trim());
            ForgeNetworkHandler.CHANNEL.sendToServer(new CnpcQuestAbandonPacket(parsedQuestId));
            return true;
        } catch (Exception e) {
            AddonLogger.LOGGER.warn("Failed to send CNPC quest abandon for " + questId + ": " + e);
            return false;
        }
    }

    public static boolean sendDialogSelected(int dialogId, int optionId) {
        try {
            Object packet = Class.forName(DIALOG_SELECTED_PACKET_CLASS)
                    .getConstructor(int.class, int.class)
                    .newInstance(dialogId, optionId);
            sendCnpcPacket(packet);
            return true;
        } catch (Exception e) {
            AddonLogger.LOGGER
                    .warn("Failed to send CNPC dialog selection " + dialogId + ":" + optionId + ": " + e);
            return false;
        }
    }

    public static boolean openMirroredDialog(Object dialog, Object npc, Object player) {
        CnpcDialogView view = toDialogView(dialog, npc, player);
        if (view == null) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            client.setScreen(new CnpcDialogueScreen(view));
        });
        return true;
    }

    public static boolean isQuestAcceptAchievement(Object packet) {
        if (!CnpcQuestIntegrationResolver.isCnpcQuestModeEnabled() || packet == null) {
            return false;
        }
        return isQuestNotification(packet, "quest.newquest");
    }

    public static boolean isQuestAcceptChat(Object packet) {
        if (!CnpcQuestIntegrationResolver.isCnpcQuestModeEnabled() || packet == null) {
            return false;
        }
        return isQuestNotification(packet, "quest.newquest");
    }

    public static boolean isQuestCompleteAchievement(Object packet) {
        if (!CnpcQuestIntegrationResolver.isCnpcQuestModeEnabled() || packet == null) {
            return false;
        }
        return isQuestNotification(packet, "quest.completed");
    }

    public static boolean isQuestCompleteChat(Object packet) {
        if (!CnpcQuestIntegrationResolver.isCnpcQuestModeEnabled() || packet == null) {
            return false;
        }
        return isQuestNotification(packet, "quest.completed");
    }

    public static List<OverlayAnnouncement> getActiveAnnouncements() {
        pruneExpiredAnnouncements();
        return List.copyOf(ACTIVE_ANNOUNCEMENTS);
    }

    public static void renderAnnouncementOverlay(DrawContext context, TextRenderer textRenderer, int screenWidth, int y) {
        pruneExpiredAnnouncements();
        if (ACTIVE_ANNOUNCEMENTS.isEmpty()) {
            return;
        }
        int x = (screenWidth - ANNOUNCEMENT_CARD_WIDTH) / 2;
        long now = Util.getMeasuringTimeMs();
        for (OverlayAnnouncement announcement : ACTIVE_ANNOUNCEMENTS) {
            float fade = MathHelper.clamp((announcement.expiresAt() - now) / 600.0f, 0.0f, 1.0f);
            int alpha = (int) (fade * 255.0f);
            int background = (Math.min(190, alpha) << 24) | 0x101010;
            int border = (alpha << 24) | 0xFFD35A;
            int titleColor = (alpha << 24) | 0xFFE07A;
            int subtitleColor = ((int) (Math.min(230, alpha)) << 24) | 0xFFFFFF;

            context.fill(x, y, x + ANNOUNCEMENT_CARD_WIDTH, y + ANNOUNCEMENT_CARD_HEIGHT, background);
            context.fill(x, y, x + ANNOUNCEMENT_CARD_WIDTH, y + 2, border);
            context.fill(x, y + ANNOUNCEMENT_CARD_HEIGHT - 2, x + ANNOUNCEMENT_CARD_WIDTH, y + ANNOUNCEMENT_CARD_HEIGHT,
                    border);
            context.fill(x, y, x + 2, y + ANNOUNCEMENT_CARD_HEIGHT, border);
            context.fill(x + ANNOUNCEMENT_CARD_WIDTH - 2, y, x + ANNOUNCEMENT_CARD_WIDTH, y + ANNOUNCEMENT_CARD_HEIGHT,
                    border);

            int titleWidth = textRenderer.getWidth(announcement.title());
            int subtitleWidth = textRenderer.getWidth(announcement.subtitle());
            context.drawTextWithShadow(textRenderer, Text.literal(announcement.title()),
                    x + (ANNOUNCEMENT_CARD_WIDTH - titleWidth) / 2, y + 16, titleColor);
            context.drawTextWithShadow(textRenderer, Text.literal(announcement.subtitle()),
                    x + (ANNOUNCEMENT_CARD_WIDTH - subtitleWidth) / 2, y + 40, subtitleColor);
            y += ANNOUNCEMENT_CARD_HEIGHT + 10;
        }
    }

    public static void renderGlobalAnnouncementOverlay(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof CnpcDialogueScreen || client.textRenderer == null) {
            return;
        }
        for (OverlayAnnouncement announcement : getActiveAnnouncements()) {
            if (RENDER_LOGGED.add(announcement.sequence())) {
                AddonLogger.LOGGER.info("CNPC quest announcement render seq=" + announcement.sequence()
                        + " screen=" + (client.currentScreen == null ? "null" : client.currentScreen.getClass().getName()));
            }
        }
        renderAnnouncementOverlay(context, client.textRenderer, client.getWindow().getScaledWidth(), 26);
    }

    private static void sendCnpcPacket(Object packet) throws Exception {
        if (sendServerMethod == null) {
            sendServerMethod = Class.forName(CNPC_PACKETS_CLASS).getMethod("sendServer", Object.class);
        }
        sendServerMethod.invoke(null, packet);
    }

    private static CnpcDialogView toDialogView(Object dialog, Object npc, Object player) {
        try {
            int dialogId = readIntField(dialog, "id", -1);
            if (dialogId < 0) {
                return null;
            }

            int entityId = npc instanceof Entity entity ? entity.getId() : -1;
            String npcName = npc instanceof Entity entity ? entity.getName().getString() : "NPC";
            String rawTitle = trim(readStringField(dialog, "title"));
            String title = normalizeDialogTitle(rawTitle, npcName);
            String speakerLine = isTechnicalKey(rawTitle) ? null : trim(rawTitle);
            String body = resolvePlayerTokens(firstNonBlank(readStringField(dialog, "text"), ""), player);
            boolean hasQuest = invokeBoolean(dialog, "hasQuest");
            String questId = null;
            String questTitle = null;
            if (hasQuest) {
                Object quest = invokeObject(dialog, "getQuest");
                if (quest != null) {
                    questId = String.valueOf(readIntField(quest, "id", -1));
                    if ("-1".equals(questId)) {
                        questId = null;
                    }
                    questTitle = resolvePlayerTokens(firstNonBlank(readStringField(quest, "title"), title), player);
                }
            }

            List<CnpcDialogView.Option> options = new ArrayList<>();
            Object rawOptions = readField(dialog, "options");
            if (rawOptions instanceof Map<?, ?> optionMap) {
                for (Object value : optionMap.values()) {
                    if (value == null) {
                        continue;
                    }
                    if (player != null && !isOptionAvailable(value, player)) {
                        continue;
                    }
                    int optionType = readIntField(value, "optionType", 1);
                    if (optionType == 2) {
                        continue;
                    }
                    int slot = readIntField(value, "slot", -1);
                    String optionText = resolvePlayerTokens(firstNonBlank(readStringField(value, "title"), "Continue"),
                            player);
                    int color = readIntField(value, "optionColor", 0xFFFFFFFF);
                    options.add(new CnpcDialogView.Option(slot, optionText, color));
                }
            }
            options.sort(Comparator.comparingInt(CnpcDialogView.Option::slot));
            return new CnpcDialogView(dialogId, entityId, npcName, title, speakerLine, body, hasQuest, questId,
                    questTitle, List.copyOf(options));
        } catch (Exception e) {
            AddonLogger.LOGGER.warn("Failed to build mirrored CNPC dialog view: " + e);
            return null;
        }
    }

    private static boolean isOptionAvailable(Object option, Object player) {
        try {
            for (Method method : option.getClass().getMethods()) {
                if (!method.getName().equals("isAvailable") || method.getParameterCount() != 1) {
                    continue;
                }
                return Boolean.TRUE.equals(method.invoke(option, player));
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    private static boolean invokeBoolean(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        Object value = method.invoke(target);
        return Boolean.TRUE.equals(value);
    }

    private static Object invokeObject(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private static Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getField(fieldName);
        return field.get(target);
    }

    private static String readStringField(Object target, String fieldName) throws Exception {
        Object value = readField(target, fieldName);
        return value == null ? null : value.toString();
    }

    private static int readIntField(Object target, String fieldName, int fallback) {
        try {
            Object value = readField(target, fieldName);
            return value instanceof Number number ? number.intValue() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean isQuestNotification(Object packet, String translationKey) {
        return componentContains(packet, "title", translationKey)
                || componentContains(packet, "message", translationKey);
    }

    private static boolean componentContains(Object packet, String fieldName, String translationKey) {
        try {
            Field field = packet.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(packet);
            if (value == null) {
                return false;
            }
            String raw = value.toString();
            if (raw.contains(translationKey)) {
                return true;
            }
            Method getString = value.getClass().getMethod("getString");
            Object stringValue = getString.invoke(value);
            if (stringValue != null) {
                String text = stringValue.toString().trim().toLowerCase(java.util.Locale.ROOT);
                if ("quest.newquest".equals(translationKey)) {
                    return text.equals("new quest") || text.equals("quest accepted");
                }
                if ("quest.completed".equals(translationKey)) {
                    return text.equals("quest completed") || text.equals("quest complete");
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static String normalizeTitle(String questTitle) {
        return firstNonBlank(questTitle, "Unknown Quest");
    }

    private static void enqueueOverlayAnnouncement(OverlayAnnouncement announcement) {
        ACTIVE_ANNOUNCEMENTS.addLast(announcement);
        AddonLogger.LOGGER.info("CNPC quest announcement enqueue seq=" + announcement.sequence()
                + " title=" + announcement.title() + " / " + announcement.subtitle());
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    private static void pruneExpiredAnnouncements() {
        long now = Util.getMeasuringTimeMs();
        while (!ACTIVE_ANNOUNCEMENTS.isEmpty() && ACTIVE_ANNOUNCEMENTS.peekFirst().expiresAt() < now) {
            OverlayAnnouncement removed = ACTIVE_ANNOUNCEMENTS.removeFirst();
            RENDER_LOGGED.remove(removed.sequence());
        }
    }

    private static String normalizeDialogTitle(String rawTitle, String npcName) {
        String normalized = trim(rawTitle);
        if (normalized == null || isTechnicalKey(normalized)) {
            return npcName;
        }
        return normalized;
    }

    private static boolean isTechnicalKey(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.indexOf(' ') < 0
                && (value.contains("_") || value.contains(".") || value.equals(value.toLowerCase(java.util.Locale.ROOT)));
    }

    private static String resolvePlayerTokens(String text, Object player) {
        String value = trim(text);
        if (value == null) {
            return null;
        }
        String playerName = null;
        if (player instanceof net.minecraft.entity.player.PlayerEntity playerEntity) {
            playerName = playerEntity.getName().getString();
        }
        if (playerName == null || playerName.isBlank()) {
            return value;
        }
        return value.replace("@p", playerName);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record OverlayAnnouncement(long sequence, String title, String subtitle, long expiresAt) {
    }
}
