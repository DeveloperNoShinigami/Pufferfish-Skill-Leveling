package net.puffish.skillsmod.server.network.packets.out;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.network.OutPacket;
import net.puffish.skillsmod.server.data.CategoryData;
import net.puffish.skillsmod.config.CategoryConfig;
import net.puffish.skillsmod.util.ExtendedPointSources;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extended version of {@link SkillUpdateOutPacket}.  In addition to the
 * standard information about the skill, this packet also includes the
 * player's point totals per source after the update.  The base packet is
 * used internally so that unchanged behaviour is delegated.
 */
public class ExtendedSkillUpdateOutPacket implements OutPacket {
    private final SkillUpdateOutPacket base;
    private final Map<Identifier, Integer> points;
    private final int spentPoints;
    private final int earnedPoints;

    /**
     * Creates a packet representing the change of a single skill as well as
     * the resulting point totals.  The totals are computed from the supplied
     * category data to ensure consistency with the server state.
     */
    public ExtendedSkillUpdateOutPacket(Identifier categoryId,
                                        String skillId,
                                        boolean unlocked,
                                        int level,
                                        CategoryConfig category,
                                        CategoryData data) {
        this.base = new SkillUpdateOutPacket(categoryId, skillId, unlocked, level);
        this.spentPoints = data.getSpentPoints(category);
        this.earnedPoints = data.getPointsTotal();
        this.points = data.getPointsSources()
                .collect(Collectors.toMap(id -> id, data::getPoints));
    }

    @Override
    public void write(PacketByteBuf buf) {
        base.write(buf);
        buf.writeInt(spentPoints);
        buf.writeInt(earnedPoints);
        ExtendedPointSources.write(buf, points);
    }

    @Override
    public Identifier getId() {
        // keep using the same packet identifier as the vanilla skill update
        return base.getId();
    }
}
