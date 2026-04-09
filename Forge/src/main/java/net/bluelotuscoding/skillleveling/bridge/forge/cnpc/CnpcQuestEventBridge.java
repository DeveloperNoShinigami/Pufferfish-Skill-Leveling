package net.bluelotuscoding.skillleveling.bridge.forge.cnpc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.bluelotuscoding.skillleveling.bridge.network.SyncCnpcQuestAnnouncementPacket;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestIntegrationResolver;
import net.bluelotuscoding.skillleveling.bridge.cnpc.runtime.CnpcRuntimeBridge;
import net.bluelotuscoding.skillleveling.network.ForgeNetworkHandler;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CnpcQuestEventBridge {
    private static final String NPC_API_CLASS = "noppes.npcs.api.NpcAPI";
    private static final Set<String> QUEST_EVENT_CLASSES = Set.of(
            "noppes.npcs.api.event.QuestEvent$QuestStartEvent",
            "noppes.npcs.api.event.QuestEvent$QuestCompletedEvent",
            "noppes.npcs.api.event.QuestEvent$QuestTurnedInEvent");
    private static final CnpcQuestEventBridge LISTENER = new CnpcQuestEventBridge();
    private static final AtomicLong ANNOUNCEMENT_SEQUENCE = new AtomicLong();

    private static boolean registrationAttempted;
    private static IEventBus registeredBus;
    private static Field playerField;
    private static Field questField;
    private static Method getMcEntityMethod;
    private static Method getQuestIdMethod;
    private static Method getQuestNameMethod;

    private CnpcQuestEventBridge() {
    }

    public static synchronized void ensureRegistered() {
        if (registeredBus != null || registrationAttempted) {
            return;
        }
        registrationAttempted = true;

        if (!CnpcQuestIntegrationResolver.isCnpcQuestModeEnabled()) {
            return;
        }

        try {
            Class<?> npcApiClass = Class.forName(NPC_API_CLASS);
            Method instanceMethod = npcApiClass.getMethod("Instance");
            Object api = instanceMethod.invoke(null);
            if (api == null) {
                return;
            }

            Method eventsMethod = npcApiClass.getMethod("events");
            Object bus = eventsMethod.invoke(api);
            if (!(bus instanceof IEventBus eventBus)) {
                return;
            }

            eventBus.register(LISTENER);
            registeredBus = eventBus;
            initializePlayerAccess();
            AddonLogger.LOGGER.info("Registered CNPC quest event bridge");
        } catch (Exception e) {
            AddonLogger.LOGGER.warn("Failed to register CNPC quest event bridge: " + e);
        }
    }

    public static synchronized void resetRegistration() {
        if (registeredBus != null) {
            try {
                registeredBus.unregister(LISTENER);
            } catch (Exception e) {
                AddonLogger.LOGGER.warn("Failed to unregister CNPC quest event bridge: " + e);
            }
        }
        registeredBus = null;
        registrationAttempted = false;
    }

    @SubscribeEvent
    public void onCnpcEvent(Event event) {
        if (event == null || !QUEST_EVENT_CLASSES.contains(event.getClass().getName())
                || !CnpcQuestIntegrationResolver.isCnpcQuestModeEnabled()) {
            return;
        }

        ServerPlayerEntity player = resolvePlayer(event);
        if (player == null || player.getServer() == null) {
            return;
        }

        String questId = resolveQuestId(event);
        String eventClass = event.getClass().getName();
        player.getServer().execute(() -> {
            if (eventClass.endsWith("QuestStartEvent")) {
                sendAnnouncement(player, resolveQuestTitle(event, questId), false);
            } else if (eventClass.endsWith("QuestTurnedInEvent")) {
                sendAnnouncement(player, resolveQuestTitle(event, questId), true);
            }
            CnpcRuntimeBridge.refreshPlayer(player);
        });
    }

    private static void initializePlayerAccess() throws Exception {
        if (playerField != null && getMcEntityMethod != null && questField != null && getQuestIdMethod != null
                && getQuestNameMethod != null) {
            return;
        }

        Class<?> questEventClass = Class.forName("noppes.npcs.api.event.QuestEvent");
        playerField = questEventClass.getField("player");
        questField = questEventClass.getField("quest");
        getMcEntityMethod = playerField.getType().getMethod("getMCEntity");
        getQuestIdMethod = questField.getType().getMethod("getId");
        getQuestNameMethod = questField.getType().getMethod("getName");
    }

    private static ServerPlayerEntity resolvePlayer(Event event) {
        try {
            initializePlayerAccess();
            Object wrappedPlayer = playerField.get(event);
            if (wrappedPlayer == null) {
                return null;
            }
            Object mcPlayer = getMcEntityMethod.invoke(wrappedPlayer);
            return mcPlayer instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
        } catch (Exception e) {
            AddonLogger.LOGGER.warn("Failed to resolve CNPC event player: " + e);
            return null;
        }
    }

    private static String resolveQuestId(Event event) {
        try {
            initializePlayerAccess();
            Object quest = questField.get(event);
            if (quest == null) {
                return null;
            }
            Object id = getQuestIdMethod.invoke(quest);
            return id == null ? null : String.valueOf(id);
        } catch (Exception e) {
            AddonLogger.LOGGER.warn("Failed to resolve CNPC event quest id: " + e);
            return null;
        }
    }

    private static String resolveQuestTitle(Event event, String questId) {
        try {
            initializePlayerAccess();
            Object quest = questField.get(event);
            if (quest == null) {
                return questId;
            }
            Object title = getQuestNameMethod.invoke(quest);
            if (title != null) {
                String value = title.toString().trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        } catch (Exception e) {
            AddonLogger.LOGGER.warn("Failed to resolve CNPC event quest title: " + e);
        }
        return questId;
    }

    private static void sendAnnouncement(ServerPlayerEntity player, String questTitle, boolean completed) {
        long sequence = ANNOUNCEMENT_SEQUENCE.incrementAndGet();
        AddonLogger.LOGGER.info("CNPC quest announcement send seq=" + sequence
                + " completed=" + completed + " title=" + questTitle);
        ForgeNetworkHandler.CHANNEL.sendTo(
                new SyncCnpcQuestAnnouncementPacket(sequence, questTitle, completed),
                player.networkHandler.connection,
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
    }
}
