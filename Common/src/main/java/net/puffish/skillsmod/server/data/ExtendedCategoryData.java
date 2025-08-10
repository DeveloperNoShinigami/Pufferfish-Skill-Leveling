package net.puffish.skillsmod.server.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.config.GeneralConfig;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link CategoryData} used when additional information
 * about a category needs to be transferred.  All behaviour that is not
 * explicitly extended is delegated to the underlying {@code CategoryData}
 * instance so existing logic remains untouched.
 */
public class ExtendedCategoryData {
    private final CategoryData delegate;

    private ExtendedCategoryData(CategoryData delegate) {
        this.delegate = delegate;
    }

    /**
     * Creates a new extended category using the same initialisation rules as
     * {@link CategoryData#create(GeneralConfig)}.
     */
    public static ExtendedCategoryData create(GeneralConfig general) {
        return new ExtendedCategoryData(CategoryData.create(general));
    }

    /**
     * Deserialises the category data from NBT.  The base implementation is
     * reused and therefore backwards compatible with vanilla save data.
     */
    public static ExtendedCategoryData read(NbtCompound nbt) {
        return new ExtendedCategoryData(CategoryData.read(nbt));
    }

    /**
     * Serialises the data back into NBT by delegating to the wrapped
     * {@link CategoryData} instance.
     */
    public NbtCompound writeNbt(NbtCompound nbt) {
        return delegate.writeNbt(nbt);
    }

    /**
     * Provides access to the underlying {@link CategoryData} for components
     * that still operate on the original type.
     */
    public CategoryData asCategoryData() {
        return delegate;
    }

    /**
     * Returns a snapshot of the point map.  Only entries with a non-zero
     * value are included.
     */
    public Map<Identifier, Integer> getPoints() {
        return delegate.getPointsSources()
                .collect(Collectors.toMap(id -> id, delegate::getPoints));
    }
}
