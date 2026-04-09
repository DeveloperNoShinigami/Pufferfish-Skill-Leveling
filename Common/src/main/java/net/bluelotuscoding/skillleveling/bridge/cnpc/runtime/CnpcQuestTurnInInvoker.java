package net.bluelotuscoding.skillleveling.bridge.cnpc.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.bluelotuscoding.skillleveling.util.AddonLogger;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CnpcQuestTurnInInvoker {
    private static final String PACKET_CLASS = "noppes.npcs.packets.server.SPacketQuestCompletionCheck";
    private static final String PACKET_BASE_CLASS = "noppes.npcs.packets.PacketServerBasic";

    private static Constructor<?> packetCtor;
    private static Field playerField;
    private static Field npcField;
    private static Method handleMethod;

    private CnpcQuestTurnInInvoker() {
    }

    public static boolean turnInQuest(ServerPlayerEntity player, int questId) {
        if (player == null || questId < 0) {
            return false;
        }
        try {
            ensureInitialized();
            Object packet = packetCtor.newInstance(questId);
            playerField.set(packet, player);
            npcField.set(packet, null);
            handleMethod.invoke(packet);
            return true;
        } catch (Exception e) {
            AddonLogger.LOGGER.warn("Failed to invoke CNPC quest turn-in for " + questId + ": " + e);
            return false;
        }
    }

    private static synchronized void ensureInitialized() throws Exception {
        if (packetCtor != null) {
            return;
        }
        Class<?> packetClass = Class.forName(PACKET_CLASS);
        Class<?> packetBaseClass = Class.forName(PACKET_BASE_CLASS);

        packetCtor = packetClass.getConstructor(int.class);
        playerField = packetBaseClass.getField("player");
        npcField = packetBaseClass.getField("npc");
        handleMethod = packetClass.getDeclaredMethod("handle");

        packetCtor.setAccessible(true);
        playerField.setAccessible(true);
        npcField.setAccessible(true);
        handleMethod.setAccessible(true);
    }
}
