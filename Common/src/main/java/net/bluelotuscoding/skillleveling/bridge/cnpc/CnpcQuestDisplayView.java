package net.bluelotuscoding.skillleveling.bridge.cnpc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public class CnpcQuestDisplayView {
    public String questId;
    public String title;
    public String body;
    public String requirementLabel;
    public String progressText;
    public String bookCategory;
    public String classId;
    public String completeText;
    public String completerNpc;
    public int questType;
    public boolean readyToTurnIn;
    public String trackStructure;
    public final List<CnpcQuestDisplayRequirement> requirements = new ArrayList<>();
    public final List<CnpcQuestDisplayReward> rewards = new ArrayList<>();

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        putString(nbt, "questId", questId);
        putString(nbt, "title", title);
        putString(nbt, "body", body);
        putString(nbt, "requirementLabel", requirementLabel);
        putString(nbt, "progressText", progressText);
        putString(nbt, "bookCategory", bookCategory);
        putString(nbt, "classId", classId);
        putString(nbt, "completeText", completeText);
        putString(nbt, "completerNpc", completerNpc);
        nbt.putInt("questType", questType);
        nbt.putBoolean("readyToTurnIn", readyToTurnIn);
        putString(nbt, "trackStructure", trackStructure);

        NbtList requirementList = new NbtList();
        for (CnpcQuestDisplayRequirement requirement : requirements) {
            requirementList.add(requirement.toNbt());
        }
        nbt.put("requirements", requirementList);

        NbtList rewardList = new NbtList();
        for (CnpcQuestDisplayReward reward : rewards) {
            rewardList.add(reward.toNbt());
        }
        nbt.put("rewards", rewardList);
        return nbt;
    }

    public static CnpcQuestDisplayView fromNbt(NbtCompound nbt) {
        CnpcQuestDisplayView view = new CnpcQuestDisplayView();
        view.questId = getString(nbt, "questId");
        view.title = getString(nbt, "title");
        view.body = getString(nbt, "body");
        view.requirementLabel = getString(nbt, "requirementLabel");
        view.progressText = getString(nbt, "progressText");
        view.bookCategory = getString(nbt, "bookCategory");
        view.classId = getString(nbt, "classId");
        view.completeText = getString(nbt, "completeText");
        view.completerNpc = getString(nbt, "completerNpc");
        view.questType = nbt.getInt("questType");
        view.readyToTurnIn = nbt.getBoolean("readyToTurnIn");
        view.trackStructure = getString(nbt, "trackStructure");

        NbtList requirementList = nbt.getList("requirements", 10);
        for (int i = 0; i < requirementList.size(); i++) {
            view.requirements.add(CnpcQuestDisplayRequirement.fromNbt(requirementList.getCompound(i)));
        }

        NbtList rewardList = nbt.getList("rewards", 10);
        for (int i = 0; i < rewardList.size(); i++) {
            view.rewards.add(CnpcQuestDisplayReward.fromNbt(rewardList.getCompound(i)));
        }
        return view;
    }

    private static void putString(NbtCompound nbt, String key, String value) {
        if (value != null && !value.isBlank()) {
            nbt.putString(key, value);
        }
    }

    private static String getString(NbtCompound nbt, String key) {
        return nbt.contains(key, 8) ? nbt.getString(key) : null;
    }
}
