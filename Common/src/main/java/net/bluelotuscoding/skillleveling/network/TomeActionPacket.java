package net.bluelotuscoding.skillleveling.network;

import net.bluelotuscoding.skillleveling.item.TomeItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Client-to-Server packet for Tome actions.
 * Sent when a player selects a skill (for Clear Mind tomes)
 * or a category (for Progression tome).
 */
public class TomeActionPacket {

    private final Identifier categoryId;
    private final String skillId; // null for Progression tome
    private final TomeItem.TomeType tomeType;

    public TomeActionPacket(Identifier categoryId, String skillId, TomeItem.TomeType tomeType) {
        this.categoryId = categoryId;
        this.skillId = skillId;
        this.tomeType = tomeType;
    }

    public Identifier getCategoryId() {
        return categoryId;
    }

    public String getSkillId() {
        return skillId;
    }

    public TomeItem.TomeType getTomeType() {
        return tomeType;
    }

    /**
     * Write packet to buffer.
     */
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(categoryId);
        buf.writeBoolean(skillId != null);
        if (skillId != null) {
            buf.writeString(skillId);
        }
        buf.writeEnumConstant(tomeType);
    }

    /**
     * Read packet from buffer.
     */
    public static TomeActionPacket read(PacketByteBuf buf) {
        Identifier categoryId = buf.readIdentifier();
        String skillId = buf.readBoolean() ? buf.readString() : null;
        TomeItem.TomeType tomeType = buf.readEnumConstant(TomeItem.TomeType.class);
        return new TomeActionPacket(categoryId, skillId, tomeType);
    }
}
