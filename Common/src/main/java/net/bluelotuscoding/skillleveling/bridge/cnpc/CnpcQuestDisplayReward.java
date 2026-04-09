package net.bluelotuscoding.skillleveling.bridge.cnpc;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class CnpcQuestDisplayReward {
    public enum Kind {
        ITEM,
        XP
    }

    public Kind kind = Kind.ITEM;
    public ItemStack stack = ItemStack.EMPTY;
    public int xp;

    public static CnpcQuestDisplayReward item(ItemStack stack) {
        CnpcQuestDisplayReward reward = new CnpcQuestDisplayReward();
        reward.kind = Kind.ITEM;
        reward.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        return reward;
    }

    public static CnpcQuestDisplayReward xp(int amount) {
        CnpcQuestDisplayReward reward = new CnpcQuestDisplayReward();
        reward.kind = Kind.XP;
        reward.xp = Math.max(0, amount);
        return reward;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("kind", kind.name());
        if (stack != null && !stack.isEmpty()) {
            nbt.put("stack", stack.writeNbt(new NbtCompound()));
        }
        nbt.putInt("xp", xp);
        return nbt;
    }

    public static CnpcQuestDisplayReward fromNbt(NbtCompound nbt) {
        CnpcQuestDisplayReward reward = new CnpcQuestDisplayReward();
        if (nbt.contains("kind", 8)) {
            try {
                reward.kind = Kind.valueOf(nbt.getString("kind"));
            } catch (IllegalArgumentException ignored) {
                reward.kind = Kind.ITEM;
            }
        }
        if (nbt.contains("stack", 10)) {
            reward.stack = ItemStack.fromNbt(nbt.getCompound("stack"));
        }
        reward.xp = nbt.getInt("xp");
        return reward;
    }
}
