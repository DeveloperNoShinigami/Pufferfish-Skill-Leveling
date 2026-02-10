package net.bluelotuscoding.skillleveling.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.bluelotuscoding.skillleveling.SkillLevelingMod;

public class RequestToggleSkillPacket {
    private final Identifier categoryId;
    private final String skillId;

    public RequestToggleSkillPacket(Identifier categoryId, String skillId) {
        this.categoryId = categoryId;
        this.skillId = skillId;
    }

    public void encode(PacketByteBuf buf) {
        buf.writeIdentifier(categoryId);
        buf.writeString(skillId);
    }

    public static RequestToggleSkillPacket decode(PacketByteBuf buf) {
        Identifier catId = buf.readIdentifier();
        String sId = buf.readString();
        return new RequestToggleSkillPacket(catId, sId);
    }

    public void handleServer(net.minecraft.server.network.ServerPlayerEntity player) {
        SkillLevelingMod.getInstance().getSkillLevelingManager().toggleSkill(player, categoryId, skillId);
    }
}
