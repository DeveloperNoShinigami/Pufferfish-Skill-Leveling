package net.bluelotuscoding.skillleveling.bridge.network;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.bluelotuscoding.skillleveling.bridge.EpicClassBridge;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassConfigManager;
import net.bluelotuscoding.skillleveling.bridge.config.EpicClassDef;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.bluelotuscoding.skillleveling.util.PuffishItemHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Replaces the core ChooseClassPacket for custom (non-enum) classes.
 * Applies health from gui_stats, gives starting items, unlocks the
 * Pufferfish skill category via EpicClassBridge, and re-evaluates restrictions.
 */
public class CustomChooseClassPacket {
    public final String customId;

    public CustomChooseClassPacket(String customId) {
        this.customId = customId == null ? "epic_classes:none" : customId;
    }

    public static void encode(CustomChooseClassPacket msg, PacketByteBuf buf) {
        buf.writeString(msg.customId);
    }

    public static CustomChooseClassPacket decode(PacketByteBuf buf) {
        return new CustomChooseClassPacket(buf.readString());
    }

    public static void handle(CustomChooseClassPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayerEntity sp = ctx.getSender();
            if (sp == null) {
                return;
            }

            if ("epic_classes:none".equalsIgnoreCase(msg.customId) || "none".equalsIgnoreCase(msg.customId)) {
                net.bluelotuscoding.skillleveling.SkillLevelingMod.getInstance().getSkillLevelingManager()
                        .resetPufferfishProgressForClassReset(sp);
            }

            // 1. Mark class as chosen in persistent NBT (Epic Class reads this on login)
            net.minecraft.nbt.NbtCompound tag = ((net.minecraftforge.common.extensions.IForgeEntity) (Object) sp)
                    .getPersistentData();
            tag.putBoolean("ecm_class_chosen", true);
            tag.putString("ecm_class_name", msg.customId);

            // 2. Load the EpicClassDef for this custom class
            EpicClassDef def = EpicClassConfigManager.getClassDef(msg.customId);

            // 3. Apply attributes from EpicClassDef.attributes & Pre-seed health
            if (def != null && def.attributes != null) {
                // Pre-seed ecm_base_max_health to trick ClassHealth.applyOnFirstChoose()
                // into skipping its default WARRIOR/SORCERER health apply.
                double customHealth = -1;
                for (var entry : def.attributes.entrySet()) {
                    if (entry.getKey().contains("generic.max_health")) {
                        customHealth = entry.getValue().value;
                        break;
                    }
                }
                if (customHealth > 0) {
                    tag.putDouble("ecm_base_max_health", customHealth);
                }

                // Targeted clear of only epic-class-specific modifiers using NBT tracking
                clearClassModifiers(sp, tag);

                // Apply all attributes via Minecraft's system
                net.minecraft.nbt.NbtList appliedAttrs = new net.minecraft.nbt.NbtList();
                net.minecraft.nbt.NbtList appliedCmds = new net.minecraft.nbt.NbtList();
                for (var entry : def.attributes.entrySet()) {
                    EpicClassDef.AttributeDef ad = entry.getValue();

                    if (ad.command != null) {
                        // Command slot: fire once on class select; KubeJS handles side-effects
                        String valStr = (ad.value == Math.floor(ad.value))
                                ? String.valueOf((long) ad.value) : String.valueOf(ad.value);
                        String cmd = ad.command
                                .replace("{value}", valStr)
                                .replace("{player}", sp.getEntityName());
                        sp.getServer().getCommandManager().executeWithPrefix(
                                sp.getServer().getCommandSource(), cmd);
                        // Track the command template (with {value}) so clearClassModifiers can undo it
                        appliedCmds.add(net.minecraft.nbt.NbtString.of(ad.command));
                        continue;
                    }

                    // NBT value override: script writes ro_attrval_<key>, config value is fallback
                    double effectiveValue = tag.contains("ro_attrval_" + entry.getKey())
                            ? tag.getInt("ro_attrval_" + entry.getKey())
                            : ad.value;

                    Identifier attrId = new Identifier(entry.getKey());
                    EntityAttribute attr = ForgeRegistries.ATTRIBUTES.getValue(attrId);
                    if (attr == null) {
                        continue;
                    }

                    EntityAttributeInstance inst = sp.getAttributeInstance(attr);
                    if (inst == null) {
                        continue;
                    }

                    String opStr = ad.operation == null ? "BASE" : ad.operation.toUpperCase();

                    if ("BASE".equals(opStr)) {
                        inst.setBaseValue(effectiveValue);
                    } else {
                        EntityAttributeModifier.Operation op = EntityAttributeModifier.Operation.ADDITION;
                        try {
                            op = EntityAttributeModifier.Operation.valueOf(opStr);
                        } catch (Exception ignored) {
                        }

                        inst.addPersistentModifier(new EntityAttributeModifier(
                                UUID.nameUUIDFromBytes(("ec_custom_" + entry.getKey()).getBytes()),
                                "epic_class_custom",
                                effectiveValue,
                                op));
                    }
                    appliedAttrs.add(net.minecraft.nbt.NbtString.of(entry.getKey()));
                }
                tag.put("ecm_applied_attributes", appliedAttrs);
                tag.put("ecm_applied_commands", appliedCmds);

                // Reset health to new max if attributes changed
                sp.setHealth(sp.getMaxHealth());
            }

            // 4. Give starting items from the EpicClassDef starting_items list (ONLY ONCE
            // PER CLASS)
            String itemsReceivedTag = "ecm_items_received_" + msg.customId.replace(":", "_");
            if (def != null && def.starting_items != null && !tag.getBoolean(itemsReceivedTag)) {
                giveStartingItems(sp, def.starting_items);
                tag.putBoolean(itemsReceivedTag, true);
            }

            // 5. Unlock Pufferfish skill category and lock others via bridge
            EpicClassBridge.onClassChanged(sp, msg.customId);

            // 6. Re-evaluate Epic Class restrictions
            try {
                Class<?> restrictionEventsClass = Class.forName("com.example.epicclassmod.event.RestrictionEvents");
                java.lang.reflect.Method recalcMethod = restrictionEventsClass.getDeclaredMethod("recalc",
                        net.minecraft.entity.player.PlayerEntity.class);
                recalcMethod.setAccessible(true);
                recalcMethod.invoke(null, (net.minecraft.entity.player.PlayerEntity) (Object) sp);
            } catch (Exception ignored) {
                // Safe to skip if Epic Class restriction system unavailable
            }

        });
    }

    /**
     * Clears all attribute modifiers previously added by the Epic Class system,
     * using the ecm_applied_attributes list in the player's persistent data.
     */
    private static void clearClassModifiers(ServerPlayerEntity sp, net.minecraft.nbt.NbtCompound tag) {
        // Re-fire command entries with {value}=0 to undo their effects
        if (tag.contains("ecm_applied_commands", 9)) {
            net.minecraft.nbt.NbtList appliedCmds = tag.getList("ecm_applied_commands", 8);
            for (int i = 0; i < appliedCmds.size(); i++) {
                String cmdTemplate = appliedCmds.getString(i);
                String cmd = cmdTemplate
                        .replace("{value}", "0")
                        .replace("{player}", sp.getEntityName());
                sp.getServer().getCommandManager().executeWithPrefix(
                        sp.getServer().getCommandSource(), cmd);
            }
            tag.remove("ecm_applied_commands");
        }

        if (!tag.contains("ecm_applied_attributes", 9)) { // 9 for NbtList
            return;
        }

        net.minecraft.nbt.NbtList appliedAttrs = tag.getList("ecm_applied_attributes", 8); // 8 for NbtString
        for (int i = 0; i < appliedAttrs.size(); i++) {
            String attrName = appliedAttrs.getString(i);
            Identifier attrId = new Identifier(attrName);
            EntityAttribute attr = ForgeRegistries.ATTRIBUTES.getValue(attrId);
            if (attr != null) {
                EntityAttributeInstance inst = sp.getAttributeInstance(attr);
                if (inst != null) {
                    UUID uuid = UUID.nameUUIDFromBytes(("ec_custom_" + attrName).getBytes());
                    inst.removeModifier(uuid);
                }
            }
        }
        tag.remove("ecm_applied_attributes");
    }

    /**
     * Parses and gives each item in the starting_items list.
     * Accepted formats per entry:
     * "namespace:item_id" — gives 1 item
     * "namespace:item_id@count" — gives `count` items
     */
    private static void giveStartingItems(ServerPlayerEntity sp, List<String> items) {
        for (String raw : items) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                String itemId = raw;
                int count = 1;
                if (raw.contains("@")) {
                    String[] parts = raw.split("@", 2);
                    itemId = parts[0].trim();
                    count = Integer.parseInt(parts[1].trim());
                }
                ItemStack stack = PuffishItemHelper.parseItemStack(itemId);
                if (stack == ItemStack.EMPTY) {
                    Identifier resLoc = new Identifier(itemId);
                    Item item = ForgeRegistries.ITEMS.getValue(resLoc);
                    if (item != null) {
                        stack = new ItemStack(item);
                    }
                }

                if (stack.isEmpty()) {
                    continue;
                }

                stack.setCount(1); // Helper returns 1 by default, but we'll manage count manually
                Item item = stack.getItem();
                // Split across multiple stacks if needed
                int maxStack = item.getMaxCount();
                while (count > 0) {
                    int give = Math.min(count, maxStack);
                    ItemStack toGive = stack.copy();
                    toGive.setCount(give);
                    sp.giveItemStack(toGive);
                    count -= give;
                }
            } catch (Exception e) {
                // Skip malformed entries silently
            }
        }
    }
}
