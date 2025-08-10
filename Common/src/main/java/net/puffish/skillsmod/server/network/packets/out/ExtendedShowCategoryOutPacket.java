package net.puffish.skillsmod.server.network.packets.out;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.config.CategoryConfig;
import net.puffish.skillsmod.network.OutPacket;
import net.puffish.skillsmod.server.data.ExtendedCategoryData;
import net.puffish.skillsmod.util.ExtendedPointSources;

import java.util.Map;

/**
 * Variant of {@link ShowCategoryOutPacket} that additionally serialises the
 * individual point sources of a category.  All base fields are written using
 * the original packet to retain compatibility and minimise duplication.
 */
public class ExtendedShowCategoryOutPacket implements OutPacket {
    private final ShowCategoryOutPacket base;
    private final ExtendedCategoryData data;

    public ExtendedShowCategoryOutPacket(CategoryConfig category, ExtendedCategoryData data) {
        this.base = new ShowCategoryOutPacket(category, data.asCategoryData());
        this.data = data;
    }

    @Override
    public void write(PacketByteBuf buf) {
        // write existing information
        base.write(buf);
        // write additional point information
        Map<Identifier, Integer> points = data.getPoints();
        ExtendedPointSources.write(buf, points);
    }

    @Override
    public Identifier getId() {
        // reuse the same packet identifier; extended clients must know how
        // to consume the additional payload.
        return base.getId();
    }
}
