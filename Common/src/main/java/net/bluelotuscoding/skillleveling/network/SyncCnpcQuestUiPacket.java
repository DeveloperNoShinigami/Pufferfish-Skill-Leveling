package net.bluelotuscoding.skillleveling.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.bluelotuscoding.skillleveling.bridge.cnpc.CnpcQuestDisplayView;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;

public class SyncCnpcQuestUiPacket {
    private final List<CnpcQuestDisplayView> quests;
    private final List<String> acceptedKeys;
    private final List<String> completedKeys;
    private final List<String> readyToTurnInKeys;

    public SyncCnpcQuestUiPacket(Collection<CnpcQuestDisplayView> quests, Collection<String> acceptedKeys,
            Collection<String> completedKeys, Collection<String> readyToTurnInKeys) {
        this.quests = quests == null ? List.of() : new ArrayList<>(quests);
        this.acceptedKeys = acceptedKeys == null ? List.of() : new ArrayList<>(acceptedKeys);
        this.completedKeys = completedKeys == null ? List.of() : new ArrayList<>(completedKeys);
        this.readyToTurnInKeys = readyToTurnInKeys == null ? List.of() : new ArrayList<>(readyToTurnInKeys);
    }

    public static void encode(SyncCnpcQuestUiPacket msg, PacketByteBuf buf) {
        NbtCompound root = new NbtCompound();
        NbtList questList = new NbtList();
        for (CnpcQuestDisplayView quest : msg.quests) {
            questList.add(quest.toNbt());
        }
        root.put("quests", questList);

        NbtList acceptedList = new NbtList();
        for (String key : msg.acceptedKeys) {
            NbtCompound tag = new NbtCompound();
            tag.putString("id", key);
            acceptedList.add(tag);
        }
        root.put("accepted", acceptedList);

        NbtList completedList = new NbtList();
        for (String key : msg.completedKeys) {
            NbtCompound tag = new NbtCompound();
            tag.putString("id", key);
            completedList.add(tag);
        }
        root.put("completed", completedList);

        NbtList readyList = new NbtList();
        for (String key : msg.readyToTurnInKeys) {
            NbtCompound tag = new NbtCompound();
            tag.putString("id", key);
            readyList.add(tag);
        }
        root.put("readyToTurnIn", readyList);

        buf.writeNbt(root);
    }

    public static SyncCnpcQuestUiPacket decode(PacketByteBuf buf) {
        NbtCompound root = buf.readNbt();
        List<CnpcQuestDisplayView> quests = new ArrayList<>();
        List<String> accepted = new ArrayList<>();
        List<String> completed = new ArrayList<>();
        List<String> readyToTurnIn = new ArrayList<>();
        if (root != null) {
            NbtList questList = root.getList("quests", 10);
            for (int i = 0; i < questList.size(); i++) {
                quests.add(CnpcQuestDisplayView.fromNbt(questList.getCompound(i)));
            }

            NbtList acceptedList = root.getList("accepted", 10);
            for (int i = 0; i < acceptedList.size(); i++) {
                accepted.add(acceptedList.getCompound(i).getString("id"));
            }

            NbtList completedList = root.getList("completed", 10);
            for (int i = 0; i < completedList.size(); i++) {
                completed.add(completedList.getCompound(i).getString("id"));
            }

            NbtList readyList = root.getList("readyToTurnIn", 10);
            for (int i = 0; i < readyList.size(); i++) {
                readyToTurnIn.add(readyList.getCompound(i).getString("id"));
            }

        }
        return new SyncCnpcQuestUiPacket(quests, accepted, completed, readyToTurnIn);
    }

    public List<CnpcQuestDisplayView> getQuests() {
        return quests;
    }

    public List<String> getAcceptedKeys() {
        return acceptedKeys;
    }

    public List<String> getCompletedKeys() {
        return completedKeys;
    }

    public List<String> getReadyToTurnInKeys() {
        return readyToTurnInKeys;
    }
}
