package net.bluelotuscoding.skillleveling.bridge.cnpc;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class CnpcQuestDisplayRequirement {
    public ItemStack stack = ItemStack.EMPTY;
    public int current;
    public int target;
    public String text;

    public CnpcQuestDisplayRequirement() {
    }

    public CnpcQuestDisplayRequirement(ItemStack stack, int current, int target, String text) {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.current = current;
        this.target = target;
        this.text = text;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        if (stack != null && !stack.isEmpty()) {
            nbt.put("stack", stack.writeNbt(new NbtCompound()));
        }
        nbt.putInt("current", current);
        nbt.putInt("target", target);
        if (text != null) {
            nbt.putString("text", text);
        }
        return nbt;
    }

    public static CnpcQuestDisplayRequirement fromNbt(NbtCompound nbt) {
        CnpcQuestDisplayRequirement req = new CnpcQuestDisplayRequirement();
        if (nbt.contains("stack", 10)) {
            req.stack = ItemStack.fromNbt(nbt.getCompound("stack"));
        }
        req.current = nbt.getInt("current");
        req.target = nbt.getInt("target");
        if (nbt.contains("text", 8)) {
            req.text = nbt.getString("text");
        }
        return req;
    }
}
