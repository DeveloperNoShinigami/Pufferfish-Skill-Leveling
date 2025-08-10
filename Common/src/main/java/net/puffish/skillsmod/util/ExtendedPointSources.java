package net.puffish.skillsmod.util;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * Utility helpers extending {@link PointSources} with support for
 * serialising and deserialising maps of point sources.  The class
 * purposely contains only static methods as it merely augments the
 * existing constants defined by {@link PointSources}.
 */
public final class ExtendedPointSources extends PointSources {
    private ExtendedPointSources() {
        // utility class
    }

    /**
     * Writes the given map of point sources to the provided buffer.
     * Entries with a zero value are still written to ensure the client
     * can faithfully reconstruct the map.
     *
     * @param buf    target buffer
     * @param points map of source identifier to amount
     */
    public static void write(PacketByteBuf buf, Map<Identifier, Integer> points) {
        buf.writeMap(points, PacketByteBuf::writeIdentifier, PacketByteBuf::writeInt);
    }

    /**
     * Reads a map of point sources from the buffer.  The map mirrors the
     * structure written by {@link #write(PacketByteBuf, Map)}.
     *
     * @param buf source buffer
     * @return reconstructed map of sources to amounts
     */
    public static Map<Identifier, Integer> read(PacketByteBuf buf) {
        return buf.readMap(PacketByteBuf::readIdentifier, PacketByteBuf::readInt);
    }
}
